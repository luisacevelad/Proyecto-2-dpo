package cds.torneos;

import java.time.LocalDate;

/**
 * Registro de que un participante ocupa un cupo en un torneo.
 */
public class InscripcionTorneo {
	private final Participante participante;
	private final LocalDate fecha;
	private final boolean preferencial;
	private final double montoPagado;

	public InscripcionTorneo(Participante participante, LocalDate fecha, boolean preferencial, double montoPagado) {
		this.participante = participante;
		this.fecha = fecha;
		this.preferencial = preferencial;
		this.montoPagado = montoPagado;
	}

	public Participante getParticipante() {
		return participante;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	/** true si ocupa uno de los cupos reservados por ranking. */
	public boolean isPreferencial() {
		return preferencial;
	}

	public double getMontoPagado() {
		return montoPagado;
	}
}
