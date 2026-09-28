import { DiaSemana, DIAS_ORDEN, JS_A_DIASEMANA } from "../constants/dias";

export interface RangoDTO {
  id: number;
  dia: DiaSemana;
  horaInicio: string;
  horaFin: string;
}

export interface CeldaCalendario {
  iso: string;
  numero: number;
}

export type SemanaCalendario = (CeldaCalendario | null)[];

export interface MesCalendario {
  label: string;
  semanas: SemanaCalendario[];
}

const MESES_CORTOS = ["Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"];

export function diaSemanaDeISO(dateStr: string): DiaSemana {
  const [y, m, d] = dateStr.split("-").map(Number);
  return JS_A_DIASEMANA[new Date(y, m - 1, d).getDay()];
}

function toIdx(d: number) {
  return d === 0 ? 6 : d - 1;
}

export function generarSlots(rangos: RangoDTO[], bloqueMinutos: number, dia: DiaSemana): string[] {
  const slots: string[] = [];
  for (const r of rangos) {
    if (r.dia !== dia) continue;
    const [hI, mI] = r.horaInicio.split(":").map(Number);
    const [hF, mF] = r.horaFin.split(":").map(Number);
    const inicio = hI * 60 + mI;
    const fin = hF * 60 + mF;
    for (let t = inicio; t + bloqueMinutos <= fin; t += bloqueMinutos) {
      const hh = Math.floor(t / 60).toString().padStart(2, "0");
      const mm = (t % 60).toString().padStart(2, "0");
      slots.push(`${hh}:${mm}`);
    }
  }
  return slots;
}

export function generarCalendario(_rangos: RangoDTO[], mesesAnticipacion: number): MesCalendario[] {
  const hoy = new Date();
  const calendario: MesCalendario[] = [];
  for (let i = 0; i < mesesAnticipacion; i++) {
    const anio = hoy.getFullYear();
    const mes = hoy.getMonth() + i;
    const primerDia = new Date(anio, mes, 1);
    const ultimoDia = new Date(anio, mes + 1, 0);
    const diasEnMes = ultimoDia.getDate();
    const diaSemanaInicio = toIdx(primerDia.getDay());
    const semanas: SemanaCalendario[] = [];
    let semana: SemanaCalendario = [];
    for (let d = 0; d < diaSemanaInicio; d++) {
      semana.push(null);
    }
    for (let dia = 1; dia <= diasEnMes; dia++) {
      const fechaStr = `${anio}-${String(mes + 1).padStart(2, "0")}-${String(dia).padStart(2, "0")}`;
      semana.push({ iso: fechaStr, numero: dia });
      if (semana.length === 7) {
        semanas.push(semana);
        semana = [];
      }
    }
    if (semana.length > 0) {
      while (semana.length < 7) {
        semana.push(null);
      }
      semanas.push(semana);
    }
    calendario.push({ label: `${MESES_CORTOS[mes]} ${anio}`, semanas });
  }
  return calendario;
}

export function emptyRangos(): Record<DiaSemana, RangoDTO[]> {
  return DIAS_ORDEN.reduce((acc, d) => ({ ...acc, [d]: [] }), {} as Record<DiaSemana, RangoDTO[]>);
}

export function agruparRangosPorDia(rangos: RangoDTO[]): Record<DiaSemana, RangoDTO[]> {
  const base = emptyRangos();
  rangos.forEach((r) => base[r.dia].push(r));
  return base;
}
