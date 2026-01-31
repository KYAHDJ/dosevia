import { TimePicker } from "@mui/x-date-pickers/TimePicker";
import { TextField } from "@mui/material";

interface Props {
  label: string;
  value: Date | null;
  onChange: (time: Date | null) => void;
}

export default function ModernTimePicker({ label, value, onChange }: Props) {
  return (
    <TimePicker
      label={label}
      value={value}
      onChange={onChange}
      slots={{ textField: TextField }}
      slotProps={{
        textField: {
          fullWidth: true,
          inputProps: { readOnly: true }
        }
      }}
    />
  );
}
