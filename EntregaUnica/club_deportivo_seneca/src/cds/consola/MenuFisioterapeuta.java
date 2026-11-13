package cds.consola;

import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;
import cds.usuarios.Fisioterapeuta;
import cds.usuarios.RestriccionMedica;
import cds.usuarios.Socio;

public class MenuFisioterapeuta {
	private final ConsolaEmpleado consola;

	public MenuFisioterapeuta(ConsolaEmpleado consola) {
		this.consola = consola;
	}

	public Menu construir() {
		return new Menu("Restricciones medicas", consola.entrada)
				.agregar("Registrar restriccion medica de un jugador", this::registrar)
				.agregar("Dar de alta a un jugador", this::darDeAlta)
				.agregar("Consultar jugadores con restriccion vigente", this::consultar);
	}

	private void registrar() throws ReglaNegocioException {
		Socio s = consola.elegirSocio(consola.socios());
		String motivo = consola.entrada.texto("Motivo de la restriccion");
		consola.club.registrarRestriccion((Fisioterapeuta) consola.usuario, s, motivo, consola.hoy());
		System.out.println(s.getNombre() + " queda inhabilitado hasta que se le de de alta.");
	}

	private List<RestriccionMedica> vigentes(Socio s) {
		List<RestriccionMedica> resultado = new ArrayList<RestriccionMedica>();
		for (RestriccionMedica r : s.getRestricciones()) {
			if (r.estaVigente()) {
				resultado.add(r);
			}
		}
		return resultado;
	}

	private List<Socio> sociosRestringidos() {
		List<Socio> resultado = new ArrayList<Socio>();
		for (Socio s : consola.socios()) {
			if (!vigentes(s).isEmpty()) {
				resultado.add(s);
			}
		}
		return resultado;
	}

	private void darDeAlta() throws ReglaNegocioException {
		List<Socio> restringidos = sociosRestringidos();
		if (restringidos.isEmpty()) {
			System.out.println("No hay jugadores inhabilitados.");
			return;
		}
		Socio s = consola.elegirSocio(restringidos);
		RestriccionMedica r = consola.entrada.elegir("Restricciones vigentes", vigentes(s),
				x -> x.getFechaInicio() + ": " + x.getMotivo());
		r.darDeAlta(consola.entrada.fechaOHoy("Fecha de alta"));
		System.out.println("Alta registrada. " + s.getNombre()
				+ (s.estaHabilitado() ? " ya puede jugar y entrenar." : " aun tiene otras restricciones."));
	}

	private void consultar() {
		List<Socio> restringidos = sociosRestringidos();
		if (restringidos.isEmpty()) {
			System.out.println("No hay jugadores inhabilitados.");
			return;
		}
		for (Socio s : restringidos) {
			for (RestriccionMedica r : vigentes(s)) {
				System.out.println("- " + s.getNombre() + ": " + r.getMotivo() + " (desde " + r.getFechaInicio() + ")");
			}
		}
	}
}
