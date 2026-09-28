import styles from "../AgendaScreen.module.css";

type TurnoDTO = {
  id: number;
  agendaId: number;
  clienteId: number | null;
  clienteNombre: string | null;
  clienteApellido: string | null;
  nombreCliente: string | null;
  clienteFotoPerfil: string | null;
  servicioId: number | null;
  servicioNombre: string | null;
  servicioPrecio: number | null;
  fecha: string;
  bloqueHorario: string;
  estado: string;
};

type Props = {
  turno: TurnoDTO;
  onCancel: (turno: TurnoDTO) => void;
  onClose: () => void;
};

export const CancelTurnoModal = ({ turno, onCancel, onClose }: Props) => {
  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modal}>
        {turno.estado === "CONFIRMADO" ? (
          <>
            <h3 className={styles.modalTitle}>¿Cancelar turno?</h3>
            <p className={styles.modalText}>
              El turno está pago. Se cancelará y el cliente será notificado por mail para que pueda reprogramarlo.
            </p>
            <div className={styles.modalActions}>
              <button className={styles.btnCancelarTurnos} onClick={() => { onCancel(turno); onClose(); }}>
                Sí, cancelar y notificar
              </button>
              <button className={styles.btnMantener} onClick={onClose}>
                No, volver
              </button>
            </div>
          </>
        ) : (
          <>
            <h3 className={styles.modalTitle}>
              {turno.estado === "OCUPADO_SIN_CONFIRMAR" ? "¿Eliminar turno?" : "¿Eliminar bloque?"}
            </h3>
            <p className={styles.modalText}>
              El turno no está pago, así que se eliminará del calendario. Esta acción no se puede deshacer.
            </p>
            <div className={styles.modalActions}>
              <button className={styles.btnCancelarTurnos} onClick={() => { onCancel(turno); onClose(); }}>
                Sí, eliminar
              </button>
              <button className={styles.btnMantener} onClick={onClose}>
                No, volver
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};
