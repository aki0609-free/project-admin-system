/* eslint-disable @typescript-eslint/no-explicit-any */
import { VCheckbox, VSelect, VTextarea, VTextField } from "vuetify/components";
import MultiSelectWithChips from "../components/form/MultiSelectWithChips.vue";
import DateFormField from "../components/form/DateFormField.vue";
import DayRuleField from "../components/form/DayRuleField.vue"
import TimeFormField from "../components/form/TimeFormField.vue";
import SqlEditorField from "../components/form/SqlEditorField.vue";

export const componentMap: Record<string, any> = {
    text: VTextField,
    password: VTextField,
    number: VTextField,
    select: VSelect,
    checkbox: VCheckbox,
    textarea: VTextarea,
    date: DateFormField,
    selectboxWithChips: MultiSelectWithChips,
    dayrule: DayRuleField,
    time: TimeFormField,
    sqlEditor: SqlEditorField,
}