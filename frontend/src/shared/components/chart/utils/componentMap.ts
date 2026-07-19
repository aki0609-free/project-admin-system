import { Bar, Doughnut, Line, Pie } from "vue-chartjs";

export const componentMap = {
    bar: Bar,
    line: Line,
    pie: Pie,
    doughnut: Doughnut,
} as const