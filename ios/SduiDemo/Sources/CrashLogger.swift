import Foundation
import Darwin
import os

// File-private global so the C-function-pointer handlers below can reach it
// without forming a capturing closure (which the Swift→C bridge rejects).
private let crashLogger = Logger(subsystem: "com.nba.sdui", category: "crash")

private func handleUncaughtException(_ exception: NSException) {
    let name = exception.name.rawValue
    let reason = exception.reason ?? "<no reason>"
    let stack = exception.callStackSymbols.joined(separator: "\n")
    crashLogger.fault("UNCAUGHT EXCEPTION: \(name, privacy: .public): \(reason, privacy: .public)\n\(stack, privacy: .public)")
    // Also write to stderr — `simctl launch --stderr=<path>` (see Makefile)
    // captures it. Belt-and-suspenders so output survives even if the
    // unified-log path is blocked during teardown.
    let banner = "\n========== SDUI iOS UNCAUGHT EXCEPTION ==========\n"
    let body = "\(name): \(reason)\n\(stack)\n"
    let footer = "=================================================\n"
    fputs(banner, stderr); fputs(body, stderr); fputs(footer, stderr)
    fflush(stderr)
}

private func handleFatalSignal(_ sig: Int32) {
    // The unified-log call is best-effort: os_log is not formally
    // async-signal-safe but usually works.
    let symbols = Thread.callStackSymbols.joined(separator: "\n")
    crashLogger.fault("FATAL SIGNAL \(sig, privacy: .public)\n\(symbols, privacy: .public)")

    // Async-signal-safe write(2) fallback. This is what actually survives
    // a SIGSEGV / SIGABRT. The bytes go to the app's stderr; the Makefile
    // launches with `--stderr=<path>` so the simulator forwards them to
    // a tailable file.
    let header = "\n=== SDUI iOS FATAL SIGNAL \(sig) ===\n"
    header.withCString { ptr in _ = write(2, ptr, strlen(ptr)) }
    for line in Thread.callStackSymbols {
        line.withCString { ptr in
            _ = write(2, ptr, strlen(ptr))
            _ = write(2, "\n", 1)
        }
    }

    // Restore the default handler and re-raise so the OS still produces
    // its standard crash report.
    signal(sig, SIG_DFL)
    raise(sig)
}

/// Installs process-wide handlers that log uncaught Objective-C exceptions
/// and fatal Unix signals (SIGABRT, SIGILL, SIGSEGV, SIGBUS, SIGFPE, SIGPIPE,
/// SIGTRAP) before the process exits. Without this, Swift crashes leave
/// nothing in the `make dev-ios-local` console — pure-Swift `fatalError`,
/// force-unwrap nil, and out-of-bounds subscripts all surface as signals,
/// not NSExceptions, and `simctl launch` detaches the app's stderr from
/// the Make terminal.
///
/// Output goes through `os.Logger` under subsystem `com.nba.sdui` so it
/// matches the `xcrun simctl spawn booted log stream --predicate
/// 'subsystem == "com.nba.sdui"'` tail in the `_dev-ios` Makefile target.
///
/// Install once, as early as possible in app launch. Idempotent.
enum CrashLogger {
    private static var installed = false

    static func install() {
        guard !installed else { return }
        installed = true

        crashLogger.info("CrashLogger installed (uncaught-exception + signal handlers)")
        trace("CrashLogger installed")

        NSSetUncaughtExceptionHandler(handleUncaughtException)

        let signals: [Int32] = [SIGABRT, SIGILL, SIGSEGV, SIGBUS, SIGFPE, SIGPIPE, SIGTRAP]
        for sig in signals {
            signal(sig, handleFatalSignal)
        }
    }

    /// Synchronous breadcrumb to stderr (the `simctl launch --stderr=<path>`
    /// channel). Unlike `os_log`, this is unbuffered + flushed immediately,
    /// so the last breadcrumb survives even when the process is killed by
    /// a path that bypasses our installed signal handlers (e.g. abort from
    /// a SwiftUI assertion thread, simctl crash-timeout teardown).
    ///
    /// Output is tagged `[SDUI-TRACE]` and timestamped (mono seconds) so
    /// `tail -F /tmp/sduidemo.stderr` shows the exact ordering of the last
    /// few operations before death.
    static func trace(_ message: @autoclosure () -> String,
                      file: StaticString = #fileID,
                      line: UInt = #line) {
        let t = ProcessInfo.processInfo.systemUptime
        let msg = "[SDUI-TRACE \(String(format: "%.3f", t))] \(file):\(line) \(message())\n"
        fputs(msg, stderr)
        fflush(stderr)
    }
}
