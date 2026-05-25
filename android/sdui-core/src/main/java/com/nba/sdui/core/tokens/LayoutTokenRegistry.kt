package com.nba.sdui.core.tokens

import org.json.JSONObject

data class FormFactorMatrix<T>(
    val phone: T,
    val tablet: T,
    val tv: T,
)

data class TypographySize(
    val phone: Int,
    val tablet: Int,
    val tv: Int,
)

data class TypographyCategorySpec(
    val familyRef: String,
    val weight: Int,
    val textCase: String,
    val lineHeight: Double,
)

data class TypographyVariantSpec(
    val categoryRef: String,
    val size: TypographySize,
)

data class ShadowSpec(
    val type: String,
    val color: String,
    val radius: Int,
    val offsetX: Int,
    val offsetY: Int,
)

object LayoutTokenRegistry {
    val spacing: Map<String, FormFactorMatrix<Int>> by lazy { loadSpacing() }
    val radius: Map<String, FormFactorMatrix<Int>> by lazy { loadRadius() }
    val typographyCategories: Map<String, TypographyCategorySpec> by lazy { loadTypographyCategories() }
    val typographyVariants: Map<String, TypographyVariantSpec> by lazy { loadTypographyVariants() }
    val motionDuration: Map<String, FormFactorMatrix<Int>> by lazy { loadMotionDuration() }
    val motionEasing: Map<String, String> by lazy { loadMotionEasing() }
    val shadows: Map<String, ShadowSpec> by lazy { loadShadows() }

    private fun loadSpacing(): Map<String, FormFactorMatrix<Int>> {
        val root = readJsonObject("tokens/spacing-tokens.json")
        return parseFormFactorIntMap(root.getJSONObject("spacing"))
    }

    private fun loadRadius(): Map<String, FormFactorMatrix<Int>> {
        val root = readJsonObject("tokens/corner-radius-tokens.json")
        return parseFormFactorIntMap(root.getJSONObject("radius"))
    }

    private fun loadTypographyCategories(): Map<String, TypographyCategorySpec> {
        val root = readJsonObject("tokens/typography-tokens.json")
        val categories = root.getJSONObject("categories")
        val result = linkedMapOf<String, TypographyCategorySpec>()
        categories.forEachObject { token, spec ->
            result[token] = TypographyCategorySpec(
                familyRef = spec.getString("familyRef"),
                weight = spec.getInt("weight"),
                textCase = spec.getString("textCase"),
                lineHeight = spec.getDouble("lineHeight"),
            )
        }
        return result
    }

    private fun loadTypographyVariants(): Map<String, TypographyVariantSpec> {
        val root = readJsonObject("tokens/typography-tokens.json")
        val variants = root.getJSONObject("variants")
        val result = linkedMapOf<String, TypographyVariantSpec>()
        variants.forEachObject { token, variant ->
            val size = variant.getJSONObject("size")
            result[token] = TypographyVariantSpec(
                categoryRef = variant.getString("categoryRef"),
                size = TypographySize(
                    phone = size.getInt("phone"),
                    tablet = size.getInt("tablet"),
                    tv = size.getInt("tv"),
                ),
            )
        }
        return result
    }

    private fun loadMotionDuration(): Map<String, FormFactorMatrix<Int>> {
        val root = readJsonObject("tokens/motion-tokens.json")
        return parseFormFactorIntMap(root.getJSONObject("duration"))
    }

    private fun loadMotionEasing(): Map<String, String> {
        val root = readJsonObject("tokens/motion-tokens.json")
        val easing = root.getJSONObject("easing")
        val result = linkedMapOf<String, String>()
        easing.forEachKey { token ->
            result[token] = easing.getString(token)
        }
        return result
    }

    private fun loadShadows(): Map<String, ShadowSpec> {
        val root = readJsonObject("tokens/shadow-tokens.json")
        val shadows = root.getJSONObject("shadows")
        val result = linkedMapOf<String, ShadowSpec>()
        shadows.forEachObject { token, spec ->
            result[token] = ShadowSpec(
                type = spec.getString("type"),
                color = spec.getString("color"),
                radius = spec.getInt("radius"),
                offsetX = spec.getInt("offsetX"),
                offsetY = spec.getInt("offsetY"),
            )
        }
        return result
    }

    private fun parseFormFactorIntMap(data: JSONObject): Map<String, FormFactorMatrix<Int>> {
        val result = linkedMapOf<String, FormFactorMatrix<Int>>()
        data.forEachObject { token, value ->
            result[token] = FormFactorMatrix(
                phone = value.getInt("phone"),
                tablet = value.getInt("tablet"),
                tv = value.getInt("tv"),
            )
        }
        return result
    }

    private fun readJsonObject(path: String): JSONObject {
        val classLoader = LayoutTokenRegistry::class.java.classLoader
            ?: throw IllegalStateException("No class loader available for token registry")
        val stream = classLoader.getResourceAsStream(path)
            ?: throw IllegalStateException("Missing token resource: $path")
        val text = try {
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (error: Exception) {
            throw IllegalStateException("Failed reading token resource: $path", error)
        }

        return try {
            JSONObject(text)
        } catch (error: Exception) {
            throw IllegalStateException("Malformed token resource: $path", error)
        }
    }

    private inline fun JSONObject.forEachObject(action: (String, JSONObject) -> Unit) {
        val iterator = keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            action(key, getJSONObject(key))
        }
    }

    private inline fun JSONObject.forEachKey(action: (String) -> Unit) {
        val iterator = keys()
        while (iterator.hasNext()) {
            action(iterator.next())
        }
    }
}
