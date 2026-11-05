// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(    
    name: "background_fetch",
    platforms: [
        // Matches TSBackgroundFetch 4.2's iOS 15; don't rely on Xcode 27 raising a lower value (WO-001)
        .iOS("15.0")
    ],
    products: [
        .library(
            name: "background-fetch",
            targets: ["background_fetch"]
        )
    ],    
    dependencies: [
        // Lower bound stays 4.1.0: TSLocationManager 4.8.x allows only TSBackgroundFetch 4.1.x, and a
        // 4.2.0 floor makes apps using both unresolvable. Standalone apps still resolve 4.2.x. (WO-001)
        .package(url: "https://github.com/transistorsoft/transistor-background-fetch.git", from: "4.1.0"),
        .package(name: "FlutterFramework", path: "../FlutterFramework")
    ],
    targets: [
        .target(
            name: "background_fetch",
            dependencies: [
                .product(name: "TSBackgroundFetch", package: "transistor-background-fetch"),
                .product(name: "FlutterFramework", package: "FlutterFramework")
            ],
            cSettings: [
                .headerSearchPath("include/background_fetch")
            ]
        )        
    ]
)
