package cds.torneos;

import java.time.LocalDate;

import cds.ReglaNegocioException;

public class EncuentroTorneo {
	private final InscripcionTorneo participanteA;
	private final InscripcionTorneo participanteB;
	private final LocalDate fecha;
	private final String ronda;
	private int marcadorA;
	private int marcadorB;
	private boolean jugado;

	public EncuentroTorneo(InscripcionTorneo participanteA, InscripcionTorneo participanteB, LocalDate fecha, String ronda)
			throws ReglaNegocioException {
		if (participanteA == participanteB) {
			throw new ReglaNegocioException("Un participante no puede jugar contra si mismo");
		}
		this.participanteA = participanteA;
		this.participanteB = participanteB;
		this.fecha = fecha;
		this.ronda = ronda;
		jugado = false;
	}

	void registrarResultado(int marcadorA, int marcadorB, boolean permiteEmpate) throws ReglaNegocioException {
		if (jugado) {
			throw new ReglaNegocioException("Este encuentro ya tiene resultado");
		}
		if (marcadorA < 0 || marcadorB < 0) {
			throw new ReglaNegocioException("El marcador no puede ser negativo");
		}
		if (marcadorA == marcadorB && !permiteEmpate) {
			throw new ReglaNegocioException("En este deporte no puede haber empate");
		}
		this.marcadorA = marcadorA;
		this.marcadorB = marcadorB;
		jugado = true;
	}

	public void restaurarResultado(int marcadorA, int marcadorB) {
		this.marcadorA = marcadorA;
		this.marcadorB = marcadorB;
		jugado = true;
	}

	public InscripcionTorneo getGanador() {
		if (!jugado || marcadorA == marcadorB) {
			return null;
		}
		if (marcadorA > marcadorB) {
			return participanteA;
		}
		return participanteB;
	}

	public boolean involucra(InscripcionTorneo i) {
		return participanteA == i || participanteB == i;
	}

	public InscripcionTorneo getParticipanteA() {
		return participanteA;
	}

	public InscripcionTorneo getParticipanteB() {
		return participanteB;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public String getRonda() {
		return ronda;
	}

	public int getMarcadorA() {
		return marcadorA;
	}

	public int getMarcadorB() {
		return marcadorB;
	}

	public boolean isJugado() {
		return jugado;
	}
}
