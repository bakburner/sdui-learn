import Foundation
import Darwin

/// Installs process-wide handlers that print uncaught Objective-C exceptions
/// and fatal Unix signals (SIGABRT, SIGILL, SIGSEGV, SIGBUS, SIGFPE, SIGPIPE,
/// SIGTRAP) to stderr before the process exits. Without this, Swift crashes
/// frequently leave nothing in the Xcode console when the app is run outside
/// the debugger — pure-Swift `fatalError`, force-unwrap nil, and out-of-bounds
/// subscripts all surface as signals, not NSExceptions.
///
/// Install once, as early as possible in app launch. Idempotent.
enum CrashLogger {
    private static var installed = false

    static func install() {
        guard !installed else { return }
        installed = true

        NSSetUncaughtExceptionHandler { exception in
            let name = exception.name.rawValue
            let reason = exception.reason ?? "<no reason>"
            let stack = exception.callStackSymbols.joined(separator: "\n")
            fputs("\n========== SDUI iOS UNCAUGHT EXCEPTION ==========\n", stderr)
            fputs("\(name): \(reason)\n\(stack)\n", stderr)
            fputs("=================================================\n", stderr)
        }

        let signals: [Int32] = [SIGABRT, SIGILL, SIGSEGV, SIGBUS, SIGFPE, SIGPIPE, SIGTRAP]
        for sig in signals {
            signal(sig) { sig in
                let symbols = Thread.callStackSymbols.joined(separator: "\n")
                fputs("\n========== SDUI iOS FATAL SIGNAL \(sig) ==========\n", stderr)
                fputs("\(symbols)\n", stderr)
                fputs("==================================================\n", stderr)
                // Restore default handler and re-raise so the OS still produces
                // the normal crash report.
                signal(sig, SIG_DFL)
                raise(sig)
            }
        }
    }
}
