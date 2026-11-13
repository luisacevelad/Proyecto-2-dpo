package cds.usuarios;

public class Externo extends Usuario {
	private final String documento;

	public Externo(String login, String password, String nombre, String documento) {
		super(login, password, nombre);
		this.documento = documento;
	}

	public String getDocumento() {
		return documento;
	}
}
