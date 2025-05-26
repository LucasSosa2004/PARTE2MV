package maquinaVirtual;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class DisassemblerV2 {
    private Registros registros;
    private MemoriaBase memoria;
    private TablaDescripSegmentosV2 tabla;
    private int entryPoint;

    public DisassemblerV2(Registros registros, MemoriaBase memoria, TablaDescripSegmentosV2 tabla) {
        this.registros = registros;
        this.memoria = memoria;
        this.tabla = tabla;
        this.entryPoint = tabla.getEntryPoint();
    }

    public int decodificarInstruccion(byte primerByte) {
        byte codOperacion = (byte) (primerByte & 0x1F);
        int bytesYaLeidosInstruccion = 1; 

        byte tipoOpA = 0, tipoOpB = 0;
        int valorOpA = 0, valorOpB = 0;
        int cantBytesOpA = 0, cantBytesOpB = 0;
        String instHexa = ""; 
        String lineaDissasembler = "";

//        // Obtener el segmento actual y su dirección base
//        String segmentoActual = tabla.getSegmentoDirFisica(registros.getDirFisicaIP());

        int ipFisica = registros.getIP();
        String segmentoActual = tabla.getSegmentoDirFisica(ipFisica);
        if (segmentoActual == null) {
            throw new IllegalStateException("No se encontró segmento para IP física: " + ipFisica);
        }
        int dirBase = tabla.getSegmento(segmentoActual).getBase();
        int offset = registros.getIP() - dirBase;
        
        if (codOperacion <= 0x0D) {  // Instrucciones de un operando 
            tipoOpA = (byte) ((primerByte >> 6) & 0x03);

            valorOpA = obtenerOpEnMemoria(tipoOpA, bytesYaLeidosInstruccion); 
            cantBytesOpA = cantidadBytesOperando(tipoOpA);
            bytesYaLeidosInstruccion += cantBytesOpA;

            instHexa = concatenarInstHexa(primerByte, valorOpA, null);
            lineaDissasembler = String.format("%s %s",
                    codOpAMnemonico(codOperacion),
                    decodificarOp(registros, tipoOpA, valorOpA));
        } 
        else if (codOperacion == 0x0B || codOperacion == 0x0C) { // PUSH y POP
            tipoOpA = (byte) ((primerByte >> 6) & 0x03);

            valorOpA = obtenerOpEnMemoria(tipoOpA, bytesYaLeidosInstruccion); 
            cantBytesOpA = cantidadBytesOperando(tipoOpA);
            bytesYaLeidosInstruccion += cantBytesOpA;

            instHexa = concatenarInstHexa(primerByte, valorOpA, null);
            lineaDissasembler = String.format("%s %s",
                    codOpAMnemonico(codOperacion),
                    decodificarOp(registros, tipoOpA, valorOpA));
        }
        else if (codOperacion >= 0x10 && codOperacion <= 0x1E) { // Instrucciones de dos operandos
            tipoOpB = (byte) ((primerByte & 0xC0) >> 6);
            tipoOpA = (byte) ((primerByte & 0x30) >> 4);

            valorOpB = obtenerOpEnMemoria(tipoOpB, bytesYaLeidosInstruccion);
            cantBytesOpB = cantidadBytesOperando(tipoOpB);
            bytesYaLeidosInstruccion += cantBytesOpB;

            valorOpA = obtenerOpEnMemoria(tipoOpA, bytesYaLeidosInstruccion);
            cantBytesOpA = cantidadBytesOperando(tipoOpA);
            bytesYaLeidosInstruccion += cantBytesOpA;

            instHexa = concatenarInstHexa(primerByte, valorOpB, valorOpA);
            lineaDissasembler = String.format("%s %s, %s",
                    codOpAMnemonico(codOperacion),
                    decodificarOp(registros, tipoOpA, valorOpA),
                    decodificarOp(registros, tipoOpB, valorOpB));
        } 
        else if (codOperacion == 0x0F) { // STOP
            instHexa = "0F";
            lineaDissasembler = "STOP";
            String entryPointMark = (registros.getIP() == entryPoint) ? "> " : "  ";
            System.out.printf("%s[%s:%04X] %-20s | %s\n", 
                    entryPointMark,
                    segmentoActual, 
                    offset, 
                    instHexa, 
                    lineaDissasembler);
            return -1; // Termina el disassembler
        }
        else if (codOperacion == 0x0E) { // RET
            instHexa = "0E";
            lineaDissasembler = "RET";
        }
        else {
            bytesYaLeidosInstruccion = -1; // instrucción inválida
        }

        // Imprimir la instrucción con información del segmento y entry point
        String entryPointMark = (registros.getIP() == entryPoint) ? "> " : "  ";
        System.out.printf("%s[%04X] %-20s | %s\n", 
                entryPointMark,
                offset, 
                instHexa, 
                lineaDissasembler);

        return bytesYaLeidosInstruccion;
    }

    public void mostrarCadenas() {
        DescriptorSegmento segmentoKS = tabla.getSegmento("KS");
        if (segmentoKS == null) return;

        int base = segmentoKS.getBase();
        int limite = segmentoKS.getTamanio();
        int pos = base;

        while (pos < limite) {
            // Buscar el inicio de una cadena (primer byte no nulo)
            while (pos < limite && memoria.leerByte(pos) == 0) {
                pos++;
            }
            if (pos >= limite) break;

            int inicioCadena = pos;
            StringBuilder hexString = new StringBuilder();
            StringBuilder asciiString = new StringBuilder();
            boolean cadenaCompleta = false;
            int bytesLeidos = 0;

            // Leer la cadena hasta encontrar el null terminator o llegar al límite
            while (pos < limite && !cadenaCompleta) {
                byte b = memoria.leerByte(pos);
                hexString.append(String.format("%02X ", b & 0xFF));
                bytesLeidos++;

                // Convertir a ASCII imprimible
                if (b >= 32 && b <= 126) {
                    asciiString.append((char)b);
                } else {
                    asciiString.append('.');
                }

                if (b == 0) {
                    cadenaCompleta = true;
                }
                pos++;
            }

            // Formatear la salida
            String hexOutput = hexString.toString().trim();
            if (bytesLeidos > 7) {
                // Tomar solo los primeros 6 bytes (18 caracteres: 6 bytes * 3 caracteres por byte)
                hexOutput = hexOutput.substring(0, 18) + " ..";
            }

            // Determinar si es el entry point
            String entryPointMark = (inicioCadena == entryPoint) ? "> " : "  ";

            System.out.printf("%s[%04X] %-20s \"%s\"\n", 
                    entryPointMark,
                    inicioCadena - base,
                    hexOutput,
                    asciiString.toString());
        }
    }

    private int obtenerOpEnMemoria(byte tipoOperando, int bytesYaLeidosInstruccion) {
        int comienzoInstruccion = registros.getIP();

        switch (tipoOperando) {
            case 0b01: // Registro (1 byte)
                return memoria.leerOperando(comienzoInstruccion, bytesYaLeidosInstruccion, 1);
            case 0b10: // Inmediato (2 bytes)
                return memoria.leerOperando(comienzoInstruccion, bytesYaLeidosInstruccion, 2);
            case 0b11: // Memoria (3 bytes)
                return memoria.leerOperando(comienzoInstruccion, bytesYaLeidosInstruccion, 3);
            default:
                return 0;
        }
    }

    private int cantidadBytesOperando(byte tipoOperando) {
        switch (tipoOperando) {
            case 0b00: return 0;  // Sin operando
            case 0b01: return 1;  // Registro (1 byte)
            case 0b10: return 2;  // Inmediato (2 bytes)
            case 0b11: return 3;  // Memoria (3 bytes)
            default: return 0;
        }
    }

    private String codOpAMnemonico(byte opcode) {
        switch (opcode) {
            case 0x0:  return "SYS";
            case 0x1:  return "JMP";
            case 0x2:  return "JZ";
            case 0x3:  return "JP";
            case 0x4:  return "JN";
            case 0x5:  return "JNZ";
            case 0x6:  return "JNP";
            case 0x7:  return "JNN";
            case 0x8:  return "NOT";
            case 0x0B: return "PUSH";
            case 0x0C: return "POP";
            case 0x0D: return "CALL";
            case 0x0E: return "RET";
            case 0x0F: return "STOP";
            case 0x10: return "MOV";
            case 0x11: return "ADD";
            case 0x12: return "SUB";
            case 0x13: return "SWAP";
            case 0x14: return "MUL";
            case 0x15: return "DIV";
            case 0x16: return "CMP";
            case 0x17: return "SHL";
            case 0x18: return "SHR";
            case 0x19: return "AND";
            case 0x1A: return "OR";
            case 0x1B: return "XOR";
            case 0x1C: return "LDL";
            case 0x1D: return "LDH";
            case 0x1E: return "RND";
            default: return "UNK";
        }
    }

    private String decodificarOp(Registros registros, byte tipoOp, int operando) {
        switch (tipoOp) {
            case 0b01: // Registro
                int aux = operando >> 4 & 0xF;
                String nombreRegCompleto = registros.getNombreRegistro(aux);
                int sector = (operando >> 2) & 0x3;
                if (sector == 0) {
                    return nombreRegCompleto;
                }
                char medio = nombreRegCompleto.charAt(1);
                String[] SUFIJOS = { "", "L", "H", "X" };
                return medio + SUFIJOS[sector];

            case 0b10: // Inmediato
                short inmediatoConSigno = (short) operando;
                return String.valueOf((int) inmediatoConSigno);

            case 0b11: // Memoria
                int codReg = (operando >> 4) & 0xF;
                String nombreReg = registros.getNombreRegistro(codReg);
                byte offsetRaw = (byte) ((operando >> 8) & 0xFF);
                int offsetConSigno = offsetRaw;
                if (offsetConSigno == 0) {
                    return "[" + nombreReg + "]";
                } else {
                    return "[" + nombreReg + " + " + offsetConSigno + "]";
                }

            default: return "";
        }
    }

    private String concatenarInstHexa(byte primerByte, int operandoA, Integer operandoB) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02X ", primerByte));
        sb.append(String.format("%04X", operandoA));
        if (operandoB != null) {
            sb.append(" ");
            sb.append(String.format("%04X", operandoB));
        }
        return sb.toString();
    }

    public void disassembleAll() {
        // Obtener el segmento CS
        DescriptorSegmento segmentoCS = tabla.getSegmento("CS");
        if (segmentoCS == null) return;

        int baseCS = segmentoCS.getBase();
        int limiteCS = segmentoCS.getTamanio();
        int ip = baseCS;

        while (ip < limiteCS) {
            registros.setIP(ip);
            byte primerByte = memoria.leerPrimerByte(ip);
            byte codOperacion = (byte) (primerByte & 0x1F);
            int bytesLeidos = decodificarInstruccion(primerByte);

            if (bytesLeidos <= 0) {
                ip++; // Si la instrucción es inválida, avanzamos un byte
                continue;
            }

            ip += bytesLeidos;

            if (codOperacion == 0x0F) { // STOP
                break;
            }
        }
    }
} 