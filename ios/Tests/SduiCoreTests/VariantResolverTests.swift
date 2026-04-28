import XCTest
@testable import SduiCore

final class ContainerVariantResolverTests: XCTestCase {

    func testResolveNilReturnsNil() {
        XCTAssertNil(ContainerVariantResolver.resolve(nil))
    }

    func testResolveEmptyReturnsNil() {
        XCTAssertNil(ContainerVariantResolver.resolve(""))
    }

    func testResolveUnknownReturnsNil() {
        XCTAssertNil(ContainerVariantResolver.resolve("nonexistent"))
    }

    func testHeroPhoneDefaultShadow() {
        let spec = ContainerVariantResolver.resolve("hero", formFactor: "phone")
        XCTAssertNotNil(spec)
        XCTAssertEqual(spec?.cornerRadius, 16)
        XCTAssertEqual(spec?.shadow?.radius, 6)
    }

    func testHeroTabletIncreasedShadow() {
        let spec = ContainerVariantResolver.resolve("hero", formFactor: "tablet")
        XCTAssertNotNil(spec)
        XCTAssertEqual(spec?.cornerRadius, 16)
        XCTAssertEqual(spec?.shadow?.radius, 8)
    }

    func testGroupedPhoneCornerRadius() {
        let spec = ContainerVariantResolver.resolve("grouped", formFactor: "phone")
        XCTAssertNotNil(spec)
        XCTAssertEqual(spec?.cornerRadius, 12)
    }

    func testGroupedTabletCornerRadius() {
        let spec = ContainerVariantResolver.resolve("grouped", formFactor: "tablet")
        XCTAssertNotNil(spec)
        XCTAssertEqual(spec?.cornerRadius, 12)
    }
}

final class ImageVariantResolverTests: XCTestCase {

    func testResolveNilReturnsNil() {
        XCTAssertNil(ImageVariantResolver.resolve(nil))
    }

    func testResolveUnknownReturnsNil() {
        XCTAssertNil(ImageVariantResolver.resolve("nonexistent"))
    }

    func testThumbnailPhoneCornerRadius() {
        let spec = ImageVariantResolver.resolve("thumbnail", formFactor: "phone")
        XCTAssertNotNil(spec)
        XCTAssertEqual(spec?.cornerRadius, 8)
    }

    func testThumbnailTabletCornerRadius() {
        let spec = ImageVariantResolver.resolve("thumbnail", formFactor: "tablet")
        XCTAssertNotNil(spec)
        XCTAssertEqual(spec?.cornerRadius, 12)
    }
}
