package com.nba.sdui.core.renderer.sections

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.adapters.FormFieldUi
import com.nba.sdui.core.renderer.adapters.FormUiModel
import com.nba.sdui.core.renderer.adapters.mapForm
import com.nba.sdui.core.state.SduiAction

private const val TAG = "FormRenderer"

/**
 * Form Renderer – Generic server-driven form for settings/filters.
 *
 * Field types: select (dropdown), segmented (button group), toggle (switch),
 * text (text input). Each field change updates screen state via onStateChange.
 * Submit button resolves paramBindings and fires a refresh action.
 */
@Composable
fun FormRenderer(
    section: SduiSection,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = mapForm(section, screenState)

    if (model == null) {
        Log.w(TAG, "Unable to parse form data for section ${section.id}")
        return
    }

    val isHorizontal = model.layout == "inline" || model.layout == "horizontal"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isHorizontal) {
            // Inline layout — fields in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                model.fields.forEach { field ->
                    Box(modifier = Modifier.weight(1f)) {
                        FormFieldRenderer(
                            field = field,
                            currentValue = screenState[field.stateKey],
                            onStateChange = onStateChange
                        )
                    }
                }
            }
        } else {
            // Vertical layout — fields stacked
            model.fields.forEach { field ->
                FormFieldRenderer(
                    field = field,
                    currentValue = screenState[field.stateKey],
                    onStateChange = onStateChange
                )
            }
        }

        // Submit button
        if (model.submitAction != null) {
            Button(
                onClick = {
                    Log.d(TAG, "Form submit: ${model.submitAction}")
                    onAction(model.submitAction)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = model.submitLabel ?: "Apply")
            }
        }
    }
}

// ── Field Router ─────────────────────────────────────────────────────

@Composable
private fun FormFieldRenderer(
    field: FormFieldUi,
    currentValue: Any?,
    onStateChange: (String, Any) -> Unit
) {
    when (field.fieldType) {
        "select", "picker" -> SelectField(field, currentValue, onStateChange)
        "segmented" -> SegmentedField(field, currentValue, onStateChange)
        "toggle" -> ToggleField(field, currentValue, onStateChange)
        "radio" -> RadioField(field, currentValue, onStateChange)
        "text" -> TextInputField(field, currentValue, onStateChange)
        "number" -> TextInputField(field, currentValue, onStateChange)
        else -> {
            Log.w(TAG, "Unknown field type: ${field.fieldType}")
            TextInputField(field, currentValue, onStateChange)
        }
    }
}

// ── Select (Dropdown) ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(
    field: FormFieldUi,
    currentValue: Any?,
    onStateChange: (String, Any) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedValue = currentValue?.toString() ?: field.defaultValue ?: ""
    val selectedLabel = field.options.find { it.value == selectedValue }?.label ?: selectedValue

    Column {
        field.label?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (!field.disabled) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                enabled = !field.disabled,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                field.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onStateChange(field.stateKey, option.value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// ── Segmented Buttons ────────────────────────────────────────────────

@Composable
private fun SegmentedField(
    field: FormFieldUi,
    currentValue: Any?,
    onStateChange: (String, Any) -> Unit
) {
    val selectedValue = currentValue?.toString() ?: field.defaultValue ?: ""

    Column {
        field.label?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            field.options.forEach { option ->
                val isSelected = option.value == selectedValue
                if (isSelected) {
                    FilledTonalButton(
                        onClick = { onStateChange(field.stateKey, option.value) },
                        modifier = Modifier.weight(1f),
                        enabled = !field.disabled
                    ) {
                        Text(option.label, maxLines = 1)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onStateChange(field.stateKey, option.value) },
                        modifier = Modifier.weight(1f),
                        enabled = !field.disabled
                    ) {
                        Text(option.label, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ── Toggle (Switch) ──────────────────────────────────────────────────

@Composable
private fun ToggleField(
    field: FormFieldUi,
    currentValue: Any?,
    onStateChange: (String, Any) -> Unit
) {
    val checked = when (currentValue) {
        is Boolean -> currentValue
        is String -> currentValue.equals("true", ignoreCase = true)
        else -> field.defaultValue?.toBooleanStrictOrNull() ?: false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        field.label?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = { newValue ->
                onStateChange(field.stateKey, newValue.toString())
            },
            enabled = !field.disabled
        )
    }
}

// ── Radio Group ──────────────────────────────────────────────────────

@Composable
private fun RadioField(
    field: FormFieldUi,
    currentValue: Any?,
    onStateChange: (String, Any) -> Unit
) {
    val selectedValue = currentValue?.toString() ?: field.defaultValue ?: ""

    Column {
        field.label?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        field.options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = option.value == selectedValue,
                    onClick = { onStateChange(field.stateKey, option.value) },
                    enabled = !field.disabled
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ── Text Input ───────────────────────────────────────────────────────

@Composable
private fun TextInputField(
    field: FormFieldUi,
    currentValue: Any?,
    onStateChange: (String, Any) -> Unit
) {
    val value = currentValue?.toString() ?: field.defaultValue ?: ""

    Column {
        field.label?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                onStateChange(field.stateKey, newValue)
            },
            enabled = !field.disabled,
            placeholder = field.placeholder?.let { { Text(it) } },
            singleLine = field.fieldType != "textarea",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
