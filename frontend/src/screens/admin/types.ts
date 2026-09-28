import { RangoDTO } from "../../utils/calendario";

export type UserPublicDTO = { id: number; nombre: string; apellido: string; email: string; fotoPerfil: string | null };

export type UserAdminDTO = {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  role: string;
  profesion: string | null;
  sector: string | null;
  fotoPerfil: string | null;
  agendaId: number | null;
  verified: boolean;
  active: boolean;
};

export type AgendaDTO = {
  id: number;
  profesionalId: number;
  bloqueMinutos: number;
  mesesAnticipacion: number;
  rangos: RangoDTO[];
};
export type AgendaDetailDTO = { agenda: AgendaDTO; turnos: TurnoDTO[] };

export type TurnoDTO = {
  id: number;
  agendaId: number;
  clienteId: number | null;
  clienteNombre: string | null;
  clienteApellido: string | null;
  nombreCliente: string | null;
  clienteFotoPerfil: string | null;
  profesionalId: number;
  profesionalNombre: string;
  profesionalApellido: string;
  fecha: string;
  bloqueHorario: string;
  estado: string;
};

export type AgendaPublicaDetailDTO = {
  agenda: AgendaDTO;
  turnos: { id: number; agendaId: number; fecha: string; bloqueHorario: string; estado: string }[];
};
