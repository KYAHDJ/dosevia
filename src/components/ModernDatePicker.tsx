import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { TextField } from "@mui/material";

interface Props {
  label: string;
  value: Date | null;
  onChange: (date: Date | null) => void;
}

export default function ModernDatePicker({ label, value, onChange }: Props) {
  return (
    <DatePicker
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
