package cds.torneos;

import java.util.List;

import cds.usuarios.Usuario;

/**
 * Lo que se inscribe en un torneo: un equipo, un jugador o una pareja.
 * Gracias a esta interfaz, Torneo trata igual a los tres.
 */
public interface Participante {

	String getNombre();

	FormatoTorneo getFormato();

	/**
	 * Las personas que juegan por este participante.
	 */
	List<Usuario> getIntegrantes();

	/**
	 * true si este usuario juega en este participante.
	 */
	boolean incluye(Usuario u);
}
