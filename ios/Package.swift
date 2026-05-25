// swift-tools-version: 5.9
// Swift Package manifest for the SduiCore runtime.
//
// The demo app in `SduiDemo/` consumes this package via `.package(path: "..")`.
// When the NBA ref app integrates SDUI, it imports the same package either via
// path dependency or once tagged, via a git URL.
//
// Mirrors `android/sdui-core` (library module) + `android/app` (demo) split.
//
// Intel-Mac / CI escape hatch: set `SDUI_DISABLE_ABLY=1` in the environment
// before `xcodebuild` / `swift build`. The Ably dependency is omitted and
// `AblyChannelManager` compiles as a no-op stub (polling-only runtime).
// Needed because `ably-cocoa`'s SPM module map fails on the x86_64 iOS
// simulator slice that Intel Macs require.

import Foundation
import PackageDescription

let disableAbly = ProcessInfo.processInfo.environment["SDUI_DISABLE_ABLY"] == "1"

let ablyPackageDependencies: [Package.Dependency] = disableAbly ? [] : [
    .package(
        url: "https://github.com/ably/ably-cocoa.git",
        from: "1.2.30"
    )
]

let ablyTargetDependencies: [Target.Dependency] = disableAbly ? [] : [
    .product(name: "Ably", package: "ably-cocoa")
]

let sduiCoreSwiftSettings: [SwiftSetting] = disableAbly ? [
    .define("SDUI_DISABLE_ABLY")
] : []

let package = Package(
    name: "SduiCore",
    platforms: [
        .iOS(.v17),
        // macOS minimum is only declared so SwiftPM can resolve Kingfisher's
        // dependency graph on the command line. The library targets iOS 17.
        .macOS(.v13)
    ],
    products: [
        .library(
            name: "SduiCore",
            targets: ["SduiCore"]
        )
    ],
    dependencies: [
        .package(
            url: "https://github.com/onevcat/Kingfisher.git",
            from: "8.0.0"
        )
    ] + ablyPackageDependencies,
    targets: [
        .target(
            name: "SduiCore",
            dependencies: [
                .product(name: "Kingfisher", package: "Kingfisher")
            ] + ablyTargetDependencies,
            path: "Sources/SduiCore",
            resources: [
                // Includes Resources/Tokens/*.json consumed by LayoutTokenRegistry.
                .process("Resources")
            ],
            swiftSettings: sduiCoreSwiftSettings
        ),
        .testTarget(
            name: "SduiCoreTests",
            dependencies: ["SduiCore"],
            path: "Tests/SduiCoreTests",
            resources: [
                .copy("Fixtures")
            ],
            swiftSettings: sduiCoreSwiftSettings
        )
    ]
)
