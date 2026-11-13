package cds.torneos;

import java.util.ArrayList;
import java.util.List;

import cds.usuarios.Usuario;

/**
 * Un jugador que se inscribe solo (torneo individual de sencillos).
 * Recibe un Usuario y no un Socio porque tambien pueden jugar empleados,
 * estudiantes y externos.
 */
public class JugadorTorneo implements Participante {
	private final Usuario jugador;

	public JugadorTorneo(Usuario jugador) {
		this.jugador = jugador;
	}

	@Override
	public String getNombre() {
		return jugador.getNombre();
	}

	@Override
	public FormatoTorneo getFormato() {
		return FormatoTorneo.INDIVIDUAL;
	}

	@Override
	public List<Usuario> getIntegrantes() {
		List<Usuario> integrantes = new ArrayList<Usuario>();
		integrantes.add(jugador);
		return integrantes;
	}

	public Usuario getJugador() {
		return jugador;
	}

	@Override
	public boolean incluye(Usuario u) {
		return getIntegrantes().contains(u);
	}
}
