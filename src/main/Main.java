package main;

import java.io.FileInputStream;

import maquinaVirtual.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.HashMap;

import maquinaVirtual.HeaderMV;
import maquinaVirtual.MaquinaVirtual;

public class Main {
    public static void main(String[] args) {
    	int memoriaKiB = 16; // valor por defecto		
        boolean disassemblerMode = false;
        List<String> parametrosPrograma = new ArrayList<>();
        
        Archivos archivos = Archivos.desdeArgs(args);

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.startsWith("m=")) {
                try {
                    memoriaKiB = Integer.parseInt(arg.substring(2));
                } catch (NumberFormatException e) {
                    System.err.println("Error: el valor de memoria no es válido.");
                    return;
                }

            } else if (arg.equals("-d")) {
                disassemblerMode = true;

            } else if (arg.equals("-p") && archivos.tieneVMX()) {
                for (int j = i + 1; j < args.length; j++) {
                    parametrosPrograma.add(args[j]);
                }
                break;
            }
        }

        if (!archivos.tieneVMX() && !archivos.tieneVMI()) {
            System.err.println("Error: se debe especificar al menos un archivo .vmx o .vmi");
            return;
        }

        try {
        	

            MaquinaVirtual MV;

            if(archivos.tieneVMX()) {
            	
            	FileInputStream fis = new FileInputStream(archivos.getVMX());
            	HeaderMV header = leerHeader(fis);
            	
            	if (!header.isValido()) {
            		System.err.println("Archivo invalido. Identificador incorrecto: " + header.getIdentificador());
            		fis.close();
            		return;
            	}
            	
	            if (header.getVersion() == 1) {
	                MV = new MaquinaVirtual(header.getTamanoCodigov1(), false);
	                cargarCodigo(fis, MV, header.getTamanoCodigov1());
	                if (disassemblerMode) {
	                    ejecutarDisassembler(MV, header.getTamanoCodigov1());
	                }
	            } else if (header.getVersion() == 2) {
	                int tamMemoria = memoriaKiB * 1024;
	                MV = new MaquinaVirtual(header, parametrosPrograma, tamMemoria, archivos, false);
	                cargarCodigo(fis, MV);
	                
	                if (disassemblerMode) {
	                    MV.getDissasemblerV2().mostrarCadenas();  // Primero mostrar cadenas
	                    MV.getDissasemblerV2().disassembleAll();  // Luego mostrar instrucciones
	                }
	            } else {
	                System.err.println("Version de VMX no soportada.");
	                fis.close();
	                return;
	            }
	            
	            if (!disassemblerMode) {
	                if (!archivos.tieneVMI()) {
	                    ejecutarPrograma(MV);
	                } else {
	                    ejecutarEnDebug(MV, archivos);
	                }
	            }
	            	
	            	
	            fis.close();
            }
            else { // no vmx pero si vmi
            	int tamMemoria = memoriaKiB * 1024;
            	HeaderMV header = new HeaderMV("VMI25", 1);
            	header.setMemoriaKiB(memoriaKiB);
            	MV = new MaquinaVirtual(header, parametrosPrograma, tamMemoria, archivos, disassemblerMode);
            	if (archivos.tieneVMI()) {
                    // Primero cargamos el estado desde el VMI
                    archivos.cargarEnMV(MV);
                    
                    // Ya no necesitamos llamar a cargarRegistrosV2 aquí porque
                    // los registros ya fueron cargados desde el VMI en cargarEnMV
                    
                    // Finalmente cargamos los parámetros del programa
                    MV.getUnidadAritmeticoLogica().cargarMain(parametrosPrograma);

                    MV.getTabla().mostrarTabla(); 
                    MV.getRegistros().mostrarRegistros();
                    MV.getMemoria().imprimirMemoria(0,30);
                }

                if (disassemblerMode) {
                    if (archivos.tieneVMI()) {
                        int tamanoCodigo = MV.getTabla().getSegmento("CS").getTamanio();
                        ejecutarDisassemblerV2(MV, tamanoCodigo);
                    } else {
                        System.err.println("Error: No se puede ejecutar el disassembler sin un archivo VMI");
                    }
                } else {
                    ejecutarEnDebug(MV,archivos);
                }
            }
            
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static HeaderMV leerHeader(FileInputStream fis) throws IOException {
        byte[] baseHeader = new byte[6];
        if (fis.read(baseHeader) != baseHeader.length) {
            throw new IOException("No se pudieron leer los primeros 6 bytes del header.");
        }

        String identificador = new String(baseHeader, 0, 5);
        int version = baseHeader[5] & 0xFF;

        HeaderMV header = new HeaderMV(identificador, version);

        switch (identificador) {
            case "VMX25":
                if (version == 1) {
                    byte[] v1Extra = new byte[2]; // bytes 6-7
                    if (fis.read(v1Extra) != v1Extra.length) {
                        throw new IOException("Header .vmx v1 incompleto.");
                    }
                    int tamanoCodigo = ((v1Extra[0] & 0xFF) << 8) | (v1Extra[1] & 0xFF);
                    header.setTamanoCodigov1(tamanoCodigo);
                } else if (version == 2) {
                    byte[] v2Extra = new byte[12]; // bytes 6-17
                    if (fis.read(v2Extra) != v2Extra.length) {
                        throw new IOException("Header .vmx v2 incompleto.");
                    }
                    
                    // Leer tamaños de segmentos en orden: CS, DS, ES, SS, KS
                    int tamanoCS = mascara2bytes(v2Extra[0], v2Extra[1]);
                    int tamanoDS = mascara2bytes(v2Extra[2], v2Extra[3]);
                    int tamanoES = mascara2bytes(v2Extra[4], v2Extra[5]);
                    int tamanoSS = mascara2bytes(v2Extra[6], v2Extra[7]);
                    int tamanoKS = mascara2bytes(v2Extra[8], v2Extra[9]);
                    
                    // Agregar segmentos en orden
                    header.agregarSegmento("CS", tamanoCS);
                    header.agregarSegmento("DS", tamanoDS);
                    header.agregarSegmento("ES", tamanoES);
                    header.agregarSegmento("SS", tamanoSS);
                    header.agregarSegmento("KS", tamanoKS);
                    
                    header.setEntryPoint(mascara2bytes(v2Extra[10], v2Extra[11]));
                } else {
                    throw new IOException("Version de .vmx no soportada: " + version);
                }
                break;

            case "VMI25":
                if (version != 1) {
                    throw new IOException("Version de .vmi no soportada: " + version);
                }
                byte[] vmiExtra = new byte[2]; // bytes 6-7
                if (fis.read(vmiExtra) != vmiExtra.length) {
                    throw new IOException("Header .vmi incompleto.");
                }
                int memoria = ((vmiExtra[0] & 0xFF) << 8) | (vmiExtra[1] & 0xFF);
                header.setMemoriaKiB(memoria);
                break;

            default:
                throw new IOException("Identificador desconocido: " + identificador);
        }

        return header;
    }
    
    private static void cargarCodigo(FileInputStream fis, MaquinaVirtual MV, int tamanoCodigo) throws IOException { //en V2 se llama con el tamano del CS
        int bytesLeidos = 0; //TODO
        int byteLeido;
        while (bytesLeidos < tamanoCodigo && (byteLeido = fis.read()) != -1) {
            MV.getMemoria().cargarByteAMemoria((byte) byteLeido, bytesLeidos);
            bytesLeidos++;
        }
    }
    private static void cargarCodigo(FileInputStream fis, MaquinaVirtual MV) throws IOException {
        // Estructura auxiliar para almacenar temporalmente los segmentos
        HashMap<String, byte[]> segmentosTemp = new HashMap<>();
        
        // Leemos el CS (Code Segment) del archivo
        DescriptorSegmento segmentoCS = MV.getTabla().getSegmento("CS");
        if (segmentoCS != null) {
            byte[] contenidoCS = new byte[segmentoCS.getTamanio()];
            int bytesLeidos = 0;
            int byteLeido;
            
            while (bytesLeidos < segmentoCS.getTamanio() && (byteLeido = fis.read()) != -1) {
                contenidoCS[bytesLeidos] = (byte)byteLeido;
                bytesLeidos++;
            }
            segmentosTemp.put("CS", contenidoCS);
        }
        
        // Leemos el KS (Const Segment) del archivo
        DescriptorSegmento segmentoKS = MV.getTabla().getSegmento("KS");
        if (segmentoKS != null) {
            byte[] contenidoKS = new byte[segmentoKS.getTamanio()];
            int bytesLeidos = 0;
            int byteLeido;
            
            while (bytesLeidos < segmentoKS.getTamanio() && (byteLeido = fis.read()) != -1) {
                contenidoKS[bytesLeidos] = (byte)byteLeido;
                bytesLeidos++;
            }
            segmentosTemp.put("KS", contenidoKS);
        }
        
        // Ahora cargamos en memoria en el orden correcto
        String[] ordenSegmentos = {"PS", "KS", "CS", "DS", "ES", "SS"};
        
        for (String nombreSegmento : ordenSegmentos) {
            DescriptorSegmento segmento = MV.getTabla().getSegmento(nombreSegmento);
            if (segmento != null && segmento.getTamanio() > 0) {
                int ptr = MV.getMemoria().getDireccionFisica(MV.getRegistros().getRegistro(nombreSegmento));
                
                if (segmentosTemp.containsKey(nombreSegmento)) {
                    // Si tenemos contenido del archivo, lo copiamos
                    byte[] contenido = segmentosTemp.get(nombreSegmento);
                    for (int i = 0; i < contenido.length; i++) {
                        MV.getMemoria().cargarByteAMemoria(contenido[i], ptr + i);
                    }
                } else {
                    // Para los demás segmentos, inicializamos con ceros
                    for (int i = 0; i < segmento.getTamanio(); i++) {
                        MV.getMemoria().cargarByteAMemoria((byte)0, ptr + i);
                    }
                }
            }
        }
    }
   
    
    private static int mascara2bytes(byte pri, byte seg) {
    	return ((pri & 0xFF) << 8) | (seg & 0xFF);    	
    }
    
    /*
    private static void inicializarRegistros(MaquinaVirtual MV) {
        int CS = MV.getRegistros().getCS();
        MV.getRegistros().setIP(CS);
    }

    private static void cargarSegmentosV2(FileInputStream fis, MaquinaVirtual MV, HeaderMV header) throws IOException {
        MV.getMemoria().cargarSegmentoDesdeArchivo("CS", fis, header.getTamanoCS());
        MV.getMemoria().cargarSegmentoDesdeArchivo("KS", fis, header.getTamanoKS());
        // DS, ES, SS se reservan pero no tienen contenido en el archivo
    }

    private static void inicializarRegistrosV2(MaquinaVirtual MV, HeaderMV header) {
        int ip = MV.getMemoria().getDireccionBaseSegmento("CS") + header.getEntryPoint();
        MV.getRegistros().setIP(ip);
        MV.getRegistros().setCS(MV.getMemoria().getDireccionBaseSegmento("CS"));
        MV.getRegistros().setDS(MV.getMemoria().getDireccionBaseSegmento("DS"));
        MV.getRegistros().setES(MV.getMemoria().getDireccionBaseSegmento("ES"));
        MV.getRegistros().setSS(MV.getMemoria().getDireccionBaseSegmento("SS"));
        MV.getRegistros().setKS(MV.getMemoria().getDireccionBaseSegmento("KS"));
        MV.getRegistros().setSP(MV.getMemoria().getDireccionBaseSegmento("SS") + header.getTamanoSS());
    }


    private static void cargarImagenVMI(String archivoVMI, MaquinaVirtual MV) throws IOException {
        try (FileInputStream fis = new FileInputStream(archivoVMI)) {
            byte[] headerVMI = new byte[8];
            fis.read(headerVMI);
            // Leer registros (64 bytes)
            for (int i = 0; i < 16; i++) {
                int reg = 0;
                for (int j = 0; j < 4; j++) {
                    reg = (reg << 8) | (fis.read() & 0xFF);
                }
                MV.getRegistros().setRegistro(i, reg);
            }
            // Tabla de descriptores (32 bytes)
            for (int i = 0; i < 8; i++) {
                int base = 0, limite = 0;
                for (int j = 0; j < 2; j++) base = (base << 8) | (fis.read() & 0xFF);
                for (int j = 0; j < 2; j++) limite = (limite << 8) | (fis.read() & 0xFF);
                MV.getMemoria().setDescriptor(i, base, limite);
            }
            // Memoria principal (resto del archivo)
            byte[] mem = MV.getMemoria().getMemoriaRaw();
            fis.read(mem, 0, mem.length);
        }
    }
    */
    private static void ejecutarDisassembler(MaquinaVirtual MV, HeaderMV header) {
        int i = 0, bytesInstruccion = 1;
        while (i < header.getTamanoCodigov1() && bytesInstruccion != -1) {
            int IP = MV.getRegistros().getIP();
            byte primerByte = MV.getMemoria().leerPrimerByte(IP);
            bytesInstruccion = MV.getDissasemblerAux().decodificarInstruccion(primerByte);
            if (bytesInstruccion > 0) {
                MV.getRegistros().modificaIP(bytesInstruccion);
                i += bytesInstruccion;
            }
        }
    } 
    private static void ejecutarDisassembler(MaquinaVirtual MV, int tamanoCodigo) {
        int i = 0, bytesInstruccion = 1;
        while (i < tamanoCodigo && bytesInstruccion != -1) {
            int IP = MV.getRegistros().getIP();
            byte primerByte = MV.getMemoria().leerPrimerByte(IP);
            bytesInstruccion = MV.getDissasemblerAux().decodificarInstruccion(primerByte);
            if (bytesInstruccion > 0) {
                MV.getRegistros().modificaIP(bytesInstruccion);
                i += bytesInstruccion;
            }
        }
    } 
    
    private static void ejecutarPrograma(MaquinaVirtual MV) {
        int bytesInstruccion = 1;
        boolean IPcayoSegm = false;
        while (!IPcayoSegm && bytesInstruccion != -1) {
        	//System.out.println("IP: "+Integer.toHexString(MV.getRegistros().getIP()));

    		//MV.getMemoria().imprimirMemoria(MV.getTabla().getSegmento("SS").getLimite()-30,32);
        	int IP = MV.getRegistros().getIP();
            byte primerByte = MV.getMemoria().leerPrimerByte(IP);
            bytesInstruccion = MV.getUnidadAritmeticoLogica().ejecutarInstruccion(primerByte);
            IPcayoSegm = MV.caidaSegmentoIP();
        }
        if (IPcayoSegm) {
            System.out.println("ERROR: Ejecucion interrumpida por caida de segmento del IP");
        }
    }
    
    private static void ejecutarEnDebug(MaquinaVirtual MV, Archivos archivos) throws IOException {
        Scanner sc = new Scanner(System.in);
        int bytesInstruccion = 1;
        boolean IPcayoSegm = false;
        boolean esperaInput = false;  // arranca ejecutando sin pausar
        boolean ejecutar = true;

        int IP;
        byte primerByte;

        System.out.println("debug");

        while (!IPcayoSegm && bytesInstruccion != -1) {
            ejecutar = true;

            if (esperaInput || MV.getUnidadAritmeticoLogica().getBreakPointAnterior()) {
                System.out.print(">>> ");
                String input = sc.nextLine();

                if (input.equals("q")) {
                    System.out.println("Ejecución finalizada.");
                    break;
                } else if (input.isEmpty()) { // Step
                    esperaInput = true;
                    archivos.guardarArchivoVMI(MV.getRegistros(), MV.getMemoria(), MV.getTabla());
                } else if (input.equals("d")) { // Continuar
                    esperaInput = false;
                } else {
                    System.out.println("Enter (step), d (continuar) o q (salir).");
                    ejecutar = false;
                }
            }

            if (ejecutar) {
                IP = MV.getRegistros().getIP();
                primerByte = MV.getMemoria().leerPrimerByte(IP);
                bytesInstruccion = MV.getUnidadAritmeticoLogica().ejecutarInstruccion(primerByte);
                IPcayoSegm = MV.caidaSegmentoIP();

                // breakpoint despues de >>d
                if (!esperaInput && MV.getUnidadAritmeticoLogica().getBreakPointAnterior()) {
                    esperaInput = true;
                }
            }
        }

        if (IPcayoSegm) {
            System.out.println("ERROR: Ejecución interrumpida por caída de segmento del IP");
        }
    }

    private static void ejecutarDisassemblerV2(MaquinaVirtual MV, int tamanoCodigo) {
        int i = 0, bytesInstruccion = 1;
        while (i < tamanoCodigo && bytesInstruccion != -1) {
            int IP = MV.getRegistros().getIP();
            byte primerByte = MV.getMemoria().leerPrimerByte(IP);
            bytesInstruccion = MV.getDissasemblerV2().decodificarInstruccion(primerByte);
            if (bytesInstruccion > 0) {
                MV.getRegistros().modificaIP(bytesInstruccion);
                i += bytesInstruccion;
            }
        }
    }

    public static String formatoBinario(int valor) {
        String binario = String.format("%32s", Integer.toBinaryString(valor)).replace(' ', '0');
        return binario.replaceAll("(.{8})(?=.)", "$1 ");
    }
    
}
