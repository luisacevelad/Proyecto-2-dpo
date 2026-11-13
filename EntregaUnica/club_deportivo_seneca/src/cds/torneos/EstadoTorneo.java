package cds.torneos;

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
