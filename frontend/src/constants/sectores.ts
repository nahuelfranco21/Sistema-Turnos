export const SECTORES = [
  { value: "SALUD", label: "Salud" },
  { value: "SALUD_MENTAL", label: "Salud mental" },
  { value: "DEPORTE", label: "Deporte" },
  { value: "EDUCACION", label: "Educación" },
  { value: "BELLEZA_Y_ESTETICA", label: "Belleza y estética" },
  { value: "NUTRICION", label: "Nutrición" },
  { value: "DERECHO", label: "Derecho" },
  { value: "TECNOLOGIA", label: "Tecnología" },
  { value: "OTROS", label: "Otros" },
];

export const SECTOR_NAMES = SECTORES.map(s => s.value);

export const SECTOR_LABEL: Record<string, string> = Object.fromEntries(
  SECTORES.map(s => [s.value, s.label])
);
