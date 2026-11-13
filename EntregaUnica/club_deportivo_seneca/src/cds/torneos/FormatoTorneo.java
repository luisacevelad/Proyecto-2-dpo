package cds.torneos;

/**
 * Que tipo de participante se inscribe en el torneo.
 */
public enum FormatoTorneo {
	CONJUNTO("Deporte de conjunto", 0),
	INDIVIDUAL("Individual", 1),
	DOBLES("Dobles", 2);

	private final String nombre;
	private final int integrantes;

	FormatoTorneo(String nombre, int integrantes) {
		this.nombre = nombre;
		this.integrantes = integrantes;
	}

	public String getNombre() {
		return nombre;
	}

	/**
	 * Numero de personas por participante. 0 significa "un equipo completo".
	 */
	public int getIntegrantes() {
		return integrantes;
	}
}
