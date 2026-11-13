package cds.torneos;

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

	public int getIntegrantes() {
		return integrantes;
	}
}
