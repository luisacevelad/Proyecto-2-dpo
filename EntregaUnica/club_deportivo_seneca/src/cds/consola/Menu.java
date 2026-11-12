package cds.consola;

import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;

/**
 * Menu numerado reutilizable. Para extenderlo basta con llamar agregar(...)
 * con el texto y la accion; la opcion 0 siempre es volver/salir.
 */
public class Menu {
	private final String titulo;
	private final Entrada entrada;
	private final List<String> textos;
	private final List<Accion> acciones;

	public Menu(String titulo, Entrada entrada) {
		this.titulo = titulo;
		this.entrada = entrada;
		textos = new ArrayList<String>();
		acciones = new ArrayList<Accion>();
	}

	public Menu agregar(String texto, Accion accion) {
		textos.add(texto);
		acciones.add(accion);
		return this;
	}

	/** Agrega una opcion que abre otro menu. */
	public Menu agregarSubmenu(String texto, Menu submenu) {
		return agregar(texto, () -> submenu.mostrar());
	}

	/** Muestra el menu hasta que el usuario elija 0. */
	public void mostrar() {
		while (true) {
			System.out.println();
			System.out.println("===== " + titulo + " =====");
			for (int i = 0; i < textos.size(); i++) {
				System.out.println((i + 1) + ". " + textos.get(i));
			}
			System.out.println("0. Volver / Salir");
			int opcion = entrada.entero("Opcion", 0, textos.size());
			if (opcion == 0) {
				return;
			}
			try {
				acciones.get(opcion - 1).ejecutar();
			} catch (ReglaNegocioException e) {
				System.out.println("No se pudo completar: " + e.getMessage());
			} catch (IllegalStateException e) {
				throw e;
			} catch (RuntimeException e) {
				System.out.println("Error inesperado: " + e.getMessage());
			}
		}
	}
}
