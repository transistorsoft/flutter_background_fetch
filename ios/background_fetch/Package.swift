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
        // 4.2.0 through every later 4.x: 4.2 is the first iOS 15 build. An app that also uses
        // background-geolocation needs a TSLocationManager that accepts 4.2 (4.8.x allows only 4.1.x). (WO-001)
        .package(url: "https://github.com/transistorsoft/transistor-background-fetch.git", from: "4.2.0"),
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
