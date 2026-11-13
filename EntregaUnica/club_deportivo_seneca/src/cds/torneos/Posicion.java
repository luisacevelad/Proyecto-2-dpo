package cds.torneos;

/**
 * Una fila de la tabla de posiciones de un torneo.
 * Victoria = 3 puntos, empate = 1, derrota = 0.
 */
public class Posicion {
	public static final int PUNTOS_VICTORIA = 3;
	public static final int PUNTOS_EMPATE = 1;

	private final InscripcionTorneo inscripcion;
	private int jugados;
	private int ganados;
	private int empatados;
	private int perdidos;
	private int aFavor;
	private int enContra;

	public Posicion(InscripcionTorneo inscripcion) {
		this.inscripcion = inscripcion;
	}

	void sumarEncuentro(int propios, int rival) {
		jugados++;
		aFavor += propios;
		enContra += rival;
		if (propios > rival) {
			ganados++;
		} else if (propios == rival) {
			empatados++;
		} else {
			perdidos++;
		}
	}

	public int getPuntos() {
		return ganados * PUNTOS_VICTORIA + empatados * PUNTOS_EMPATE;
	}

	public int getDiferencia() {
		return aFavor - enContra;
	}

	public InscripcionTorneo getInscripcion() {
		return inscripcion;
	}

	public Participante getParticipante() {
		return inscripcion.getParticipante();
	}

	public int getJugados() {
		return jugados;
	}

	public int getGanados() {
		return ganados;
	}

	public int getEmpatados() {
		return empatados;
	}

	public int getPerdidos() {
		return perdidos;
	}

	public int getAFavor() {
		return aFavor;
	}

	public int getEnContra() {
		return enContra;
	}
}
