package cds.consola;

import cds.usuarios.Administrador;
import cds.usuarios.Usuario;

/**
 * Consola del administrador. El menu principal solo agrupa; cada area
 * (personal, deportes, tienda) vive en su propia clase para que sea facil de extender.
 */
public class ConsolaAdministrador extends ConsolaBase {

	public static void main(String[] args) {
		new ConsolaAdministrador().iniciar();
	}

	@Override
	protected boolean aceptaUsuario(Usuario u) {
		return u instanceof Administrador;
	}

	@Override
	protected String nombreRol() {
		return "Administrador";
	}

	Administrador administrador() {
		return (Administrador) usuario;
	}

	@Override
	protected Menu construirMenu() {
		Menu menu = new Menu("Administrador - " + usuario.getNombre(), entrada);
		menu.agregarSubmenu("Usuarios y turnos", new MenuAdminPersonal(this).construir());
		menu.agregarSubmenu("Equipos, academias y competencias", new MenuAdminDeportes(this).construir());
		menu.agregarSubmenu("Tienda, cafeteria y reportes", new MenuAdminTienda(this).construir());
		// TODO Torneos (Persona A): submenu para crear torneos, consultar su estado e historial
		return menu;
	}
}
