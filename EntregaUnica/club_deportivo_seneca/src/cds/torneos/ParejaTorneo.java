package cds.torneos;

import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;
import cds.usuarios.Usuario;

/**
 * Dos jugadores que se inscriben juntos en un torneo de dobles.
 */
public class ParejaTorneo implements Participante {
	private final Usuario jugador1;
	private final Usuario jugador2;

	public ParejaTorneo(Usuario jugador1, Usuario jugador2) throws ReglaNegocioException {
		if (jugador1 == null || jugador2 == null) {
			throw new ReglaNegocioException("La pareja debe tener dos jugadores");
		}
		if (jugador1 == jugador2) {
			throw new ReglaNegocioException("Una pareja necesita dos jugadores distintos");
		}
		this.jugador1 = jugador1;
		this.jugador2 = jugador2;
	}

	@Override
	public String getNombre() {
		return jugador1.getNombre() + " / " + jugador2.getNombre();
	}

	@Override
	public FormatoTorneo getFormato() {
		return FormatoTorneo.DOBLES;
	}

	@Override
	public List<Usuario> getIntegrantes() {
		List<Usuario> integrantes = new ArrayList<Usuario>();
		integrantes.add(jugador1);
		integrantes.add(jugador2);
		return integrantes;
	}

	public Usuario getJugador1() {
		return jugador1;
	}

	public Usuario getJugador2() {
		return jugador2;
	}

	@Override
	public boolean incluye(Usuario u) {
		return getIntegrantes().contains(u);
	}
}
