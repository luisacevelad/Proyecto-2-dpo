package cds.consola;

import cds.usuarios.Administrador;
import cds.usuarios.Usuario;

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
		menu.agregarSubmenu("Torneos", new MenuAdminTorneos(this).construir());
		return menu;
	}
}
