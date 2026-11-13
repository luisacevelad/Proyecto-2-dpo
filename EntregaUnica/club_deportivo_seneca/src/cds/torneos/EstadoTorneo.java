package cds.torneos;

/**
 * Estados por los que pasa un torneo.
 * INSCRIPCIONES_ABIERTAS: se puede entrar y salir libremente.
 * EN_CURSO: ya inicio, se programan y juegan encuentros.
 * FINALIZADO: ya hay ganador y se entrego el premio.
 */
public enum EstadoTorneo {
	INSCRIPCIONES_ABIERTAS("Inscripciones abiertas"),
	EN_CURSO("En curso"),
	FINALIZADO("Finalizado");

	private final String nombre;

	EstadoTorneo(String nombre) {
		this.nombre = nombre;
	}

	public String getNombre() {
		return nombre;
	}
}
