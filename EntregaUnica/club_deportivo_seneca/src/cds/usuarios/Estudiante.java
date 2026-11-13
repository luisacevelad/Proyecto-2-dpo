package cds.usuarios;


public class Estudiante extends Usuario {
	private final String codigoEstudiantil;

	public Estudiante(String login, String password, String nombre, String codigoEstudiantil) {
		super(login, password, nombre);
		this.codigoEstudiantil = codigoEstudiantil;
	}

	public String getCodigoEstudiantil() {
		return codigoEstudiantil;
	}
}
