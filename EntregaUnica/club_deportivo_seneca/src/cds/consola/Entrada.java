package cds.consola;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

import cds.ReglaNegocioException;

/**
 * Lee y valida todas las entradas de la consola. Ningun metodo devuelve un valor
 * invalido: si el usuario se equivoca, se le vuelve a preguntar.
 */
public class Entrada {
	private final Scanner scanner;

	public Entrada(Scanner scanner) {
		this.scanner = scanner;
	}

	private String leerLinea(String mensaje) {
		System.out.print(mensaje + ": ");
		if (!scanner.hasNextLine()) {
			throw new IllegalStateException("Se cerro la entrada de la consola");
		}
		return scanner.nextLine().trim();
	}

	public String texto(String mensaje) {
		while (true) {
			String valor = leerLinea(mensaje);
			if (!valor.isEmpty()) {
				return valor;
			}
			System.out.println("  El valor no puede estar vacio.");
		}
	}

	public String textoOpcional(String mensaje) {
		return leerLinea(mensaje + " (Enter para omitir)");
	}

	public int entero(String mensaje, int minimo, int maximo) {
		while (true) {
			String valor = leerLinea(mensaje + " [" + minimo + "-" + maximo + "]");
			try {
				int numero = Integer.parseInt(valor);
				if (numero >= minimo && numero <= maximo) {
					return numero;
				}
				System.out.println("  Debe estar entre " + minimo + " y " + maximo + ".");
			} catch (NumberFormatException e) {
				System.out.println("  Escriba un numero entero.");
			}
		}
	}

	public double decimal(String mensaje, double minimo, double maximo) {
		while (true) {
			String valor = leerLinea(mensaje).replace(',', '.');
			try {
				double numero = Double.parseDouble(valor);
				if (numero >= minimo && numero <= maximo) {
					return numero;
				}
				System.out.println("  Debe estar entre " + minimo + " y " + maximo + ".");
			} catch (NumberFormatException e) {
				System.out.println("  Escriba un numero (ej: 15000 o 0.5).");
			}
		}
	}

	public LocalDate fecha(String mensaje) {
		while (true) {
			String valor = leerLinea(mensaje + " (AAAA-MM-DD)");
			try {
				return LocalDate.parse(valor);
			} catch (DateTimeParseException e) {
				System.out.println("  Fecha invalida. Ejemplo: 2026-11-20");
			}
		}
	}

	/** Igual que fecha(), pero con Enter devuelve la fecha de hoy. */
	public LocalDate fechaOHoy(String mensaje) {
		while (true) {
			String valor = leerLinea(mensaje + " (AAAA-MM-DD, Enter = hoy)");
			if (valor.isEmpty()) {
				return LocalDate.now();
			}
			try {
				return LocalDate.parse(valor);
			} catch (DateTimeParseException e) {
				System.out.println("  Fecha invalida. Ejemplo: 2026-11-20");
			}
		}
	}

	public LocalTime hora(String mensaje) {
		while (true) {
			String valor = leerLinea(mensaje + " (HH:MM)");
			try {
				return LocalTime.parse(valor.length() == 4 ? "0" + valor : valor);
			} catch (DateTimeParseException e) {
				System.out.println("  Hora invalida. Ejemplo: 08:30");
			}
		}
	}

	public boolean siNo(String mensaje) {
		while (true) {
			String valor = leerLinea(mensaje + " (s/n)").toLowerCase();
			if (valor.equals("s") || valor.equals("si")) {
				return true;
			}
			if (valor.equals("n") || valor.equals("no")) {
				return false;
			}
			System.out.println("  Responda s o n.");
		}
	}

	/**
	 * Muestra una lista numerada y devuelve el elemento elegido.
	 * Lanza ReglaNegocioException si la lista esta vacia, para que el menu lo informe.
	 */
	public <T> T elegir(String titulo, List<T> opciones, Function<T, String> descripcion) throws ReglaNegocioException {
		if (opciones.isEmpty()) {
			throw new ReglaNegocioException("No hay " + titulo.toLowerCase() + " disponibles");
		}
		System.out.println(titulo + ":");
		for (int i = 0; i < opciones.size(); i++) {
			System.out.println("  " + (i + 1) + ". " + descripcion.apply(opciones.get(i)));
		}
		int indice = entero("Seleccione", 1, opciones.size());
		return opciones.get(indice - 1);
	}

	public <E extends Enum<E>> E elegirEnum(String titulo, E[] valores) {
		System.out.println(titulo + ":");
		for (int i = 0; i < valores.length; i++) {
			System.out.println("  " + (i + 1) + ". " + valores[i].name());
		}
		return valores[entero("Seleccione", 1, valores.length) - 1];
	}
}
