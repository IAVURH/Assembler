package de.neemann.assembler.asm;

import de.neemann.assembler.expression.Context;
import de.neemann.assembler.expression.Expression;
import de.neemann.assembler.expression.ExpressionException;
import de.neemann.assembler.expression.Identifier;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author hneemann
 */
public class Program {

    private final ArrayList<Instruction> prog;
    private final Context context;
    private String labelPending;

    public Program() {
        prog = new ArrayList<>();
        context = new Context();
    }

    public Program add(Opcode opcode, int dest, int source, Expression expr) throws InstructionException {
        if ((opcode.getImmedNeeded() == Opcode.ImmedNeeded.No) && (expr != null))
            throw new InstructionException(opcode.name() + " does not need a constant");
        if ((opcode.getImmedNeeded() == Opcode.ImmedNeeded.Yes) && (expr == null))
            throw new InstructionException(opcode.name() + " needs a constant");

        Instruction i = new Instruction(opcode, dest, source, expr);
        if (labelPending != null) {
            i.setLabel(labelPending);
            labelPending = null;
        }
        prog.add(i);
        return this;
    }

    public Program add(Opcode opcode, int reg) throws InstructionException {
        switch (opcode.getRegsNeeded()) {
            case source:
                return add(opcode, 0, reg, null);
            case dest:
                return add(opcode, reg, 0, null);
            case none:
                throw new InstructionException(opcode.name() + " does not need a register");
            default:
                throw new InstructionException(opcode.name() + " needs both registers");
        }
    }

    public Program add(Opcode opcode, int dest, int source) throws InstructionException {
        if (opcode.getRegsNeeded() != Opcode.RegsNeeded.both)
            throw new InstructionException(opcode.name() + " needs both registers");

        return add(opcode, dest, source, null);
    }


    public Program add(Opcode opcode, int reg, Expression expr) throws InstructionException {
        switch (opcode.getRegsNeeded()) {
            case source:
                return add(opcode, 0, reg, expr);
            case dest:
                return add(opcode, reg, 0, expr);
            case none:
                throw new InstructionException(opcode.name() + " does not need a register");
            default:
                throw new InstructionException(opcode.name() + " needs both registers");
        }
    }

    public Program add(Opcode opcode, Expression expr) throws InstructionException {
        if (opcode.getRegsNeeded() != Opcode.RegsNeeded.none)
            throw new InstructionException(opcode.name() + " does not need a register");

        return add(opcode, 0, 0, expr);
    }

    public Program label(String label) {
        labelPending = label;
        return this;
    }

    public Program writeHex(String filename) throws IOException, ExpressionException {
        try (PrintStream out = new PrintStream(filename)) {
            writeHex(out);
        }
        return this;
    }

    public Program writeHex(final PrintStream out) throws ExpressionException {
        out.println("v2.0 raw");

        int addr = 0;
        for (Instruction in : prog) {
            in.createMachineCode(context.setAddr(addr), new MachineCodeListener() {
                @Override
                public void add(int code) {
                    out.println(Integer.toHexString(code));
                }
            });
            addr += in.size();
        }
        return this;
    }

    public Program link() throws ExpressionException {
        context.clear();

        int addr = 0;
        for (Instruction i : prog) {
            if (i.getLabel() != null) {
                context.addIdentifier(i.getLabel(), addr);
            }
            addr += i.size();
        }

        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Instruction i : prog) {
            sb.append(i.toString()).append("\n");
        }
        return sb.toString();
    }

    private Program print(PrintStream out) {
        out.print(toString());
        return this;
    }

    public static void main(String[] args) throws IOException, ExpressionException, InstructionException {
        new Program()
                .add(Opcode.XOR, 0, 0)
                .label("L1").add(Opcode.INC, 0)
                .add(Opcode.BRNZ, new Identifier("L1"))
                .link()
                .writeHex("/home/hneemann/Dokumente/DHBW/Technische_Informatik_I/Vorlesung/06_Prozessoren/java/assembler3/z.asm.hex")
                .print(System.out);
    }


}
