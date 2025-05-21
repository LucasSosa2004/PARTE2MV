package main;

import java.io.FileInputStream;

import maquinaVirtual.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
	            } else if (header.getVersion() == 2) {
	                int tamMemoria = memoriaKiB * 1024;
	                MV = new MaquinaVirtual(header, parametrosPrograma, tamMemoria,archivos,false); // usa MemoriaV2 internamente
	                cargarCodigo(fis,MV);

	                MV.getTabla().mostrarTabla();
	                MV.getRegistros().mostrarRegistros();
	                MV.getMemoria().imprimirMemoria(MV.getTabla().getSegmento("SS").getLimite()-30,32);
	                
	            } else {
	                System.err.println("Version de VMX no soportada.");
	                fis.close();
	                return;
	            }
	            
	            if(disassemblerMode) {
	            	int tamanoCodigo = header.getTamanoCodigov1();
	            	ejecutarDisassembler(MV,tamanoCodigo);
	            }else {
	            	if (!archivos.tieneVMI()){
	            		ejecutarPrograma(MV);
	            		MV.getRegistros().mostrarRegistros();
	            	}
	            	else{
	            		ejecutarEnDebug(MV, archivos);
	            	}
	            }
	            	
	            	
	            fis.close();
            }
            else { // no vmx pero si vmi
            	int tamMemoria = memoriaKiB * 1024;
            	MV = new MaquinaVirtual(parametrosPrograma,tamMemoria,archivos,disassemblerMode);
            	if (archivos.tieneVMI()) {
                    archivos.cargarEnMV(MV); 
                    MV.getUnidadAritmeticoLogica().cargarMain(parametrosPrograma);

                    MV.getTabla().mostrarTabla(); 
                    MV.getRegistros().mostrarRegistros();
                    MV.getMemoria().imprimirMemoria(0,30);
                }

                if (disassemblerMode) {
                	int tamanoCodigo = MV.getTabla().getSegmento("CS").getTamanio();
            		ejecutarDisassembler(MV, tamanoCodigo);
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
                    //System.out.println("tamano codigo = " + tamanoCodigo);
                } else if (version == 2) {
                    byte[] v2Extra = new byte[12]; // bytes 6-17
                    if (fis.read(v2Extra) != v2Extra.length) {
                        throw new IOException("Header .vmx v2 incompleto.");
                    }
                    header.agregarSegmento("CS", mascara2bytes(v2Extra[0], v2Extra[1]));
                    header.agregarSegmento("DS", mascara2bytes(v2Extra[2], v2Extra[3]));
                    header.agregarSegmento("ES", mascara2bytes(v2Extra[4], v2Extra[5]));
                    header.agregarSegmento("SS", mascara2bytes(v2Extra[6], v2Extra[7]));
                    header.agregarSegmento("KS", mascara2bytes(v2Extra[8], v2Extra[9]));
                    /*
                    header.setTamanoCS(((v2Extra[0] & 0xFF) << 8) | (v2Extra[1] & 0xFF));
                    header.setTamanoDS(((v2Extra[2] & 0xFF) << 8) | (v2Extra[3] & 0xFF));
                    header.setTamanoES(((v2Extra[4] & 0xFF) << 8) | (v2Extra[5] & 0xFF));
                    header.setTamanoSS(((v2Extra[6] & 0xFF) << 8) | (v2Extra[7] & 0xFF));
                    header.setTamanoKS(((v2Extra[8] & 0xFF) << 8) | (v2Extra[9] & 0xFF));
                    */
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
    private static void cargarCodigo(FileInputStream fis, MaquinaVirtual MV) throws IOException { //en V2 se llama con el tamano del CS
        int ptrCS = MV.getMemoria().getDireccionFisica(MV.getRegistros().getCS()); //TODO
        int byteLeido;
        int limite = MV.getTabla().getSegmento("CS").getTamanio(); 
        while (ptrCS < limite && (byteLeido = fis.read()) != -1) {
            MV.getMemoria().cargarByteAMemoria((byte)byteLeido, ptrCS);
            ptrCS++;
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


    

    public static String formatoBinario(int valor) {
        String binario = String.format("%32s", Integer.toBinaryString(valor)).replace(' ', '0');
        return binario.replaceAll("(.{8})(?=.)", "$1 ");
    }
    
}
