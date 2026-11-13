package cds.torneos;

import java.time.LocalDate;

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

	public boolean isPreferencial() {
		return preferencial;
	}

	public double getMontoPagado() {
		return montoPagado;
	}
}
