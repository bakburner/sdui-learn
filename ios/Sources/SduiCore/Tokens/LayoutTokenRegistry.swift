import Foundation

public struct FormFactorMatrix<T> {
    public let phone: T
    public let tablet: T
    public let tv: T

    public init(phone: T, tablet: T, tv: T) {
        self.phone = phone
        self.tablet = tablet
        self.tv = tv
    }
}

public struct TypographySize {
    public let phone: Int
    public let tablet: Int
    public let tv: Int
}

public struct TypographyCategorySpec {
    public let familyRef: String
    public let weight: Int
    public let textCase: String
    public let lineHeight: Double
}

public struct TypographyVariantSpec {
    public let categoryRef: String
    public let size: TypographySize
}

public struct ShadowSpec {
    public let type: String
    public let color: String
    public let radius: Int
    public let offsetX: Int
    public let offsetY: Int
}

public enum LayoutTokenRegistry {
    public static let spacing: [String: FormFactorMatrix<Int>] = loadSpacing()
    public static let radius: [String: FormFactorMatrix<Int>] = loadRadius()
    public static let typographyCategories: [String: TypographyCategorySpec] = loadTypographyCategories()
    public static let typographyVariants: [String: TypographyVariantSpec] = loadTypographyVariants()
    public static let motionDuration: [String: FormFactorMatrix<Int>] = loadMotionDuration()
    public static let motionEasing: [String: String] = loadMotionEasing()
    public static let shadows: [String: ShadowSpec] = loadShadows()

    private static func loadSpacing() -> [String: FormFactorMatrix<Int>] {
        let root = loadRootObject(from: "spacing-tokens")
        guard let spacingObject = root["spacing"] as? [String: Any] else {
            fatalError("LayoutTokenRegistry: missing spacing object in spacing-tokens.json")
        }

        return buildDictionary(spacingObject, context: "spacing-tokens.json spacing") { key, value in
            parseFormFactorMatrix(from: value, context: "spacing token \(key)")
        }
    }

    private static func loadRadius() -> [String: FormFactorMatrix<Int>] {
        let root = loadRootObject(from: "corner-radius-tokens")
        guard let radiusObject = root["radius"] as? [String: Any] else {
            fatalError("LayoutTokenRegistry: missing radius object in corner-radius-tokens.json")
        }

        return buildDictionary(radiusObject, context: "corner-radius-tokens.json radius") { key, value in
            parseFormFactorMatrix(from: value, context: "radius token \(key)")
        }
    }

    private static func loadTypographyCategories() -> [String: TypographyCategorySpec] {
        let root = loadRootObject(from: "typography-tokens")
        guard let categoriesObject = root["categories"] as? [String: Any] else {
            fatalError("LayoutTokenRegistry: missing categories object in typography-tokens.json")
        }

        return buildDictionary(categoriesObject, context: "typography-tokens.json categories") { key, value in
            guard let category = value as? [String: Any] else {
                fatalError("LayoutTokenRegistry: invalid category object for token \(key)")
            }

            return TypographyCategorySpec(
                familyRef: requireString(category, key: "familyRef", context: "typography category \(key)"),
                weight: requireInt(category, key: "weight", context: "typography category \(key)"),
                textCase: requireString(category, key: "textCase", context: "typography category \(key)"),
                lineHeight: requireDouble(category, key: "lineHeight", context: "typography category \(key)")
            )
        }
    }

    private static func loadTypographyVariants() -> [String: TypographyVariantSpec] {
        let root = loadRootObject(from: "typography-tokens")
        guard let variantsObject = root["variants"] as? [String: Any] else {
            fatalError("LayoutTokenRegistry: missing variants object in typography-tokens.json")
        }

        return buildDictionary(variantsObject, context: "typography-tokens.json variants") { key, value in
            guard let variant = value as? [String: Any] else {
                fatalError("LayoutTokenRegistry: invalid variant object for token \(key)")
            }

            guard let sizeObject = variant["size"] as? [String: Any] else {
                fatalError("LayoutTokenRegistry: missing size for typography variant \(key)")
            }

            return TypographyVariantSpec(
                categoryRef: requireString(variant, key: "categoryRef", context: "typography variant \(key)"),
                size: TypographySize(
                    phone: requireInt(sizeObject, key: "phone", context: "typography variant \(key) size"),
                    tablet: requireInt(sizeObject, key: "tablet", context: "typography variant \(key) size"),
                    tv: requireInt(sizeObject, key: "tv", context: "typography variant \(key) size")
                )
            )
        }
    }

    private static func loadMotionDuration() -> [String: FormFactorMatrix<Int>] {
        let root = loadRootObject(from: "motion-tokens")
        guard let durationObject = root["duration"] as? [String: Any] else {
            fatalError("LayoutTokenRegistry: missing duration object in motion-tokens.json")
        }

        return buildDictionary(durationObject, context: "motion-tokens.json duration") { key, value in
            parseFormFactorMatrix(from: value, context: "motion duration token \(key)")
        }
    }

    private static func loadMotionEasing() -> [String: String] {
        let root = loadRootObject(from: "motion-tokens")
        guard let easingObject = root["easing"] as? [String: Any] else {
            fatalError("LayoutTokenRegistry: missing easing object in motion-tokens.json")
        }

        return buildDictionary(easingObject, context: "motion-tokens.json easing") { key, value in
            guard let easing = value as? String else {
                fatalError("LayoutTokenRegistry: invalid easing value for token \(key)")
            }
            return easing
        }
    }

    private static func loadShadows() -> [String: ShadowSpec] {
        let root = loadRootObject(from: "shadow-tokens")
        guard let shadowsObject = root["shadows"] as? [String: Any] else {
            fatalError("LayoutTokenRegistry: missing shadows object in shadow-tokens.json")
        }

        return buildDictionary(shadowsObject, context: "shadow-tokens.json shadows") { key, value in
            guard let shadow = value as? [String: Any] else {
                fatalError("LayoutTokenRegistry: invalid shadow object for token \(key)")
            }

            return ShadowSpec(
                type: requireString(shadow, key: "type", context: "shadow token \(key)"),
                color: requireString(shadow, key: "color", context: "shadow token \(key)"),
                radius: requireInt(shadow, key: "radius", context: "shadow token \(key)"),
                offsetX: requireInt(shadow, key: "offsetX", context: "shadow token \(key)"),
                offsetY: requireInt(shadow, key: "offsetY", context: "shadow token \(key)")
            )
        }
    }

    private static func loadRootObject(from resourceName: String) -> [String: Any] {
        guard let url = Bundle.module.url(forResource: resourceName, withExtension: "json", subdirectory: "Tokens")
            ?? Bundle.module.url(forResource: resourceName, withExtension: "json")
        else {
            fatalError("LayoutTokenRegistry: missing bundled resource \(resourceName).json")
        }

        let data: Data
        do {
            data = try Data(contentsOf: url)
        } catch {
            fatalError("LayoutTokenRegistry: failed reading \(resourceName).json: \(error)")
        }

        let json: Any
        do {
            json = try JSONSerialization.jsonObject(with: data, options: [])
        } catch {
            fatalError("LayoutTokenRegistry: failed parsing \(resourceName).json: \(error)")
        }

        guard let root = json as? [String: Any] else {
            fatalError("LayoutTokenRegistry: expected top-level object in \(resourceName).json")
        }

        return root
    }

    private static func parseFormFactorMatrix(from value: Any, context: String) -> FormFactorMatrix<Int> {
        guard let object = value as? [String: Any] else {
            fatalError("LayoutTokenRegistry: expected object for \(context)")
        }

        return FormFactorMatrix(
            phone: requireInt(object, key: "phone", context: context),
            tablet: requireInt(object, key: "tablet", context: context),
            tv: requireInt(object, key: "tv", context: context)
        )
    }

    private static func requireString(_ object: [String: Any], key: String, context: String) -> String {
        guard let value = object[key] as? String else {
            fatalError("LayoutTokenRegistry: expected string for key '\(key)' in \(context)")
        }
        return value
    }

    private static func requireInt(_ object: [String: Any], key: String, context: String) -> Int {
        guard let number = object[key] as? NSNumber else {
            fatalError("LayoutTokenRegistry: expected int for key '\(key)' in \(context)")
        }
        return number.intValue
    }

    private static func requireDouble(_ object: [String: Any], key: String, context: String) -> Double {
        guard let number = object[key] as? NSNumber else {
            fatalError("LayoutTokenRegistry: expected number for key '\(key)' in \(context)")
        }
        return number.doubleValue
    }

    private static func buildDictionary<T>(
        _ object: [String: Any],
        context: String,
        transform: (_ key: String, _ value: Any) -> T
    ) -> [String: T] {
        var result: [String: T] = [:]
        result.reserveCapacity(object.count)
        for (key, value) in object {
            result[key] = transform(key, value)
        }
        if result.count != object.count {
            fatalError("LayoutTokenRegistry: failed to map dictionary for \(context)")
        }
        return result
    }
}
