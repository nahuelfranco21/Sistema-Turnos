export function formatFecha(dateStr: string): string {
  const [y, m, d] = dateStr.split("-");
  return `${d}/${m}/${y}`;
}

export function initiales(nombre: string | null, apellido: string | null): string {
  return `${nombre?.[0] ?? ""}${apellido?.[0] ?? ""}`.toUpperCase();
}

export function labelEstado(estado: string): string {
  const labels: Record<string, string> = {
    OCUPADO_SIN_CONFIRMAR: "Pendiente de pago",
    CONFIRMADO: "Confirmado",
    CANCELADO: "Cancelado",
    DESHABILITADO: "No disponible",
    REPROGRAMAR: "Pendiente de reprogramación",
  };
  return labels[estado] ?? estado.replace(/_/g, " ");
}
