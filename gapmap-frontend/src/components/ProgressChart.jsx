import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from "recharts";
import { date, percent, Empty } from "./UI";
const colors = [
  "#267461",
  "#ec9a42",
  "#7587c4",
  "#be6d8d",
  "#55a4a3",
  "#9171b4",
];
export default function ProgressChart({
  attempts = [],
  topics = [],
  height = 280,
}) {
  const rows = attempts.map((a, i) => ({
    name: `${date(a.submittedAt)} · ${i + 1}`,
    ...Object.fromEntries(a.topics.map((t) => [t.topic, t.scorePercent])),
  }));
  return rows.length ? (
    <div style={{ width: "100%", height, minWidth: 0 }}>
      <ResponsiveContainer>
        <LineChart
          data={rows}
          margin={{ top: 12, right: 20, bottom: 8, left: -18 }}
        >
          <CartesianGrid
            strokeDasharray="3 5"
            vertical={false}
            stroke="#e4e9e5"
          />
          <XAxis
            dataKey="name"
            tick={{ fontSize: 11 }}
            axisLine={false}
            tickLine={false}
          />
          <YAxis
            domain={[0, 100]}
            tick={{ fontSize: 11 }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip formatter={(v) => percent(v)} />
          <Legend iconType="circle" wrapperStyle={{ fontSize: 11 }} />
          {topics.map((t, i) => (
            <Line
              key={t.topicId}
              type="monotone"
              dataKey={t.topic}
              stroke={colors[i % colors.length]}
              strokeWidth={2.5}
              dot={{ r: 3 }}
              connectNulls={false}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  ) : (
    <Empty title="Every step counts">
      Your progress will appear after your first assessment.
    </Empty>
  );
}
