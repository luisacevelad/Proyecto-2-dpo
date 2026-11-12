package cds.consola;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import cds.Club;
import cds.ReglaNegocioException;
import cds.academias.Academia;
import cds.academias.Instalacion;
import cds.competencias.Competencia;
import cds.conjunto.Equipo;
import cds.persistencia.ArchivoClub;
import cds.usuarios.Empleado;
import cds.usuarios.Socio;
import cds.usuarios.Usuario;

/**
 * Plantilla comun de las tres consolas: carga los datos, autentica al usuario,
 * muestra el menu del rol y guarda los cambios en los mismos archivos al salir.
 * Cada consola concreta solo define que usuarios acepta y que menu muestra.
 */
public abstract class ConsolaBase {
	/** Carpeta predeterminada de datos (relativa a la carpeta del proyecto). */
	public static final String CARPETA_DATOS = "datos";
	private static final int INTENTOS_LOGIN = 3;

	protected Club club;
	protected Entrada entrada;
	protected Usuario usuario;

	/** Indica si este tipo de usuario puede usar la consola. */
	protected abstract boolean aceptaUsuario(Usuario u);

	/** Nombre del rol para los mensajes. */
	protected abstract String nombreRol();

	/** Construye el menu principal del rol para el usuario autenticado. */
	protected abstract Menu construirMenu();

	public void iniciar() {
		entrada = new Entrada(new Scanner(System.in));
		try {
			club = ArchivoClub.cargar(CARPETA_DATOS);
		} catch (IOException | ReglaNegocioException e) {
			System.out.println("No se pudieron cargar los datos de '" + CARPETA_DATOS + "': " + e.getMessage());
			System.out.println("Ejecute el programa con la carpeta del proyecto como directorio de trabajo.");
			return;
		}
		System.out.println("Club Deportivo Seneca - Consola de " + nombreRol());
		if (!autenticar()) {
			System.out.println("Demasiados intentos fallidos. Hasta luego.");
			return;
		}
		System.out.println("Bienvenido(a), " + usuario.getNombre());
		try {
			construirMenu().mostrar();
		} catch (IllegalStateException e) {
			System.out.println(e.getMessage());
		}
		guardar();
	}

	private boolean autenticar() {
		for (int intento = 1; intento <= INTENTOS_LOGIN; intento++) {
			String login = entrada.texto("Login");
			String password = entrada.texto("Password");
			try {
				Usuario u = club.autenticar(login, password);
				if (aceptaUsuario(u)) {
					usuario = u;
					return true;
				}
				System.out.println("Este usuario no puede entrar a la consola de " + nombreRol() + ".");
			} catch (ReglaNegocioException e) {
				System.out.println(e.getMessage());
			}
		}
		return false;
	}

	private void guardar() {
		try {
			ArchivoClub.guardar(club, CARPETA_DATOS);
			System.out.println("Cambios guardados en " + CARPETA_DATOS + ". Hasta luego.");
		} catch (IOException e) {
			System.out.println("ERROR: no se pudieron guardar los cambios: " + e.getMessage());
		}
	}

	// ----------------------------------------------------------------------
	// Selectores comunes (los usan varios menus)
	// ----------------------------------------------------------------------

	protected LocalDate hoy() {
		return LocalDate.now();
	}

	protected List<Socio> socios() {
		List<Socio> resultado = new ArrayList<Socio>();
		for (Usuario u : club.getUsuarios()) {
			if (u instanceof Socio) {
				resultado.add((Socio) u);
			}
		}
		return resultado;
	}

	protected List<Empleado> empleados() {
		List<Empleado> resultado = new ArrayList<Empleado>();
		for (Usuario u : club.getUsuarios()) {
			if (u instanceof Empleado) {
				resultado.add((Empleado) u);
			}
		}
		return resultado;
	}

	protected Socio elegirSocio(List<Socio> candidatos) throws ReglaNegocioException {
		return entrada.elegir("Socios", candidatos, s -> s.getNombre() + " (" + s.getLogin() + ")");
	}

	protected Equipo elegirEquipo(List<Equipo> equipos) throws ReglaNegocioException {
		return entrada.elegir("Equipos", equipos, e -> e.getNombre() + " - " + e.getDeporte().getNombre() + " "
				+ e.getCategoria().getNombre() + " (" + e.getMiembros().size() + "/" + e.getCupoMaximo() + ")");
	}

	protected Academia elegirAcademia(List<Academia> academias) throws ReglaNegocioException {
		return entrada.elegir("Academias", academias, a -> a.getNombre() + " - " + a.getDisciplina());
	}

	protected Instalacion elegirInstalacion(List<Instalacion> instalaciones) throws ReglaNegocioException {
		return entrada.elegir("Instalaciones", instalaciones,
				i -> i.getNombre() + " (" + i.getTipo() + (i.isTechada() ? ", techada" : "") + ")");
	}

	protected Competencia elegirCompetencia() throws ReglaNegocioException {
		return entrada.elegir("Competencias", club.getCompetencias(), c -> c.getNombre() + " - "
				+ c.getDeporte().getNombre() + " " + c.getCategoria().getNombre() + " (" + c.getTemporada() + ")");
	}

	/** Todas las instalaciones conocidas del club (de academias). */
	protected List<Instalacion> instalaciones() {
		List<Instalacion> resultado = new ArrayList<Instalacion>();
		for (Academia a : club.getAcademias()) {
			for (Instalacion i : a.getInstalaciones()) {
				if (!resultado.contains(i)) {
					resultado.add(i);
				}
			}
		}
		return resultado;
	}

	protected static String pesos(double valor) {
		return String.format("$%,.0f", valor);
	}
}
