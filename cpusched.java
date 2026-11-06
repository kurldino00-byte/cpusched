import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Main extends JFrame {
    class P { String id; int at, bt, pr, rem, ct, start; }
    String[] algos = {"FCFS", "SJF", "Priority", "Round Robin"};
    List<P> list = new ArrayList<>();
    DefaultTableModel model = new DefaultTableModel(new String[]{"Process ID", "AT", "BT", "Priority"}, 0);
    JTextField idBox = new JTextField(5), atBox = new JTextField(4), btBox = new JTextField(4), prBox = new JTextField(4), qBox = new JTextField("2", 4);
    JComboBox<String> algoBox = new JComboBox<>(algos);
    JTextArea area = new JTextArea();
    String top, bottom, log, table;

    void schedule(String algo, int q) {
        top = "|"; bottom = ""; log = "";
        List<P> waiting = new ArrayList<>(list), ready = new ArrayList<>();
        waiting.sort((a, b) -> a.at - b.at);
        for (P p : list) { p.rem = p.bt; p.start = -1; }
        int time = 0;
        while (!waiting.isEmpty() || !ready.isEmpty()) {
            while (!waiting.isEmpty() && waiting.get(0).at <= time) ready.add(waiting.remove(0));
            if (ready.isEmpty()) {
                int next = waiting.get(0).at;
                top += " Idle |"; bottom += String.format("%-7d", time);
                log += "Time " + time + " to " + next + ": CPU idle\n";
                time = next;
                continue;
            }
            P p = ready.get(0);
            for (P x : ready) {
                if (algo.equals("SJF") && x.bt < p.bt) p = x;
                if (algo.equals("Priority") && x.pr < p.pr) p = x;
            }
            ready.remove(p);
            int run = p.rem;
            if (algo.equals("Round Robin") && q < run) run = q;
            if (p.start < 0) p.start = time;
            top += " " + p.id + " |"; bottom += String.format("%-" + (p.id.length() + 3) + "d", time);
            time += run;
            p.rem -= run;
            log += "Time " + (time - run) + " to " + time + ": " + p.id + " running" + (p.rem > 0 ? " (preempted)" : " (finished)") + "\n";
            while (!waiting.isEmpty() && waiting.get(0).at <= time) ready.add(waiting.remove(0));
            if (p.rem > 0) ready.add(p); else p.ct = time;
        }
        bottom += time;
    }

    double[] summary() {
        double wt = 0, tat = 0;
        table = String.format("%-10s%-5s%-5s%-5s%-6s%-5s%-5s\n", "Process", "AT", "BT", "CT", "TAT", "WT", "RT");
        for (P p : list) {
            int t = p.ct - p.at, w = t - p.bt;
            wt += w; tat += t;
            table += String.format("%-10s%-5d%-5d%-5d%-6d%-5d%-5d\n", p.id, p.at, p.bt, p.ct, t, w, p.start - p.at);
        }
        return new double[]{wt / list.size(), tat / list.size()};
    }

    void error(String message) { JOptionPane.showMessageDialog(this, message, "Invalid Input", JOptionPane.ERROR_MESSAGE); }

    void addProcess() {
        try {
            P p = new P();
            p.id = idBox.getText().trim();
            p.at = Integer.parseInt(atBox.getText().trim());
            p.bt = Integer.parseInt(btBox.getText().trim());
            p.pr = prBox.getText().trim().isEmpty() ? 1 : Integer.parseInt(prBox.getText().trim());
            boolean used = p.id.equalsIgnoreCase("Idle");
            for (P x : list) if (x.id.equalsIgnoreCase(p.id)) used = true;
            if (p.id.isEmpty() || used || p.at < 0 || p.bt <= 0 || p.pr < 0) throw new NumberFormatException();
            list.add(p);
            model.addRow(new Object[]{p.id, p.at, p.bt, p.pr});
            idBox.setText(""); atBox.setText(""); btBox.setText(""); prBox.setText("");
            idBox.requestFocus();
        } catch (NumberFormatException e) {
            error("Enter a unique Process ID, AT (0 or more), BT (1 or more), and a whole-number Priority.");
        }
    }

    void simulate(boolean compare) {
        int q = 0;
        try { q = Integer.parseInt(qBox.getText().trim()); } catch (NumberFormatException e) { }
        if (list.isEmpty() || q <= 0) { error("Add at least one process and use a time quantum of 1 or more."); return; }
        String text = "";
        for (String a : algos) {
            if (!compare && !a.equals(algoBox.getSelectedItem())) continue;
            schedule(a, q);
            double[] r = summary();
            if (compare) text += String.format("%s: Avg WT = %.2f, Avg TAT = %.2f\n", a, r[0], r[1]);
            else text = "Gantt Chart (" + a + ")\n" + top + "\n" + bottom + "\n\n" + table + "\n" + log
                + String.format("\nAverage Waiting Time: %.2f\nAverage Turnaround Time: %.2f", r[0], r[1]);
        }
        if (compare) JOptionPane.showMessageDialog(this, text, "Algorithm Comparison", JOptionPane.PLAIN_MESSAGE);
        else { area.setText(text); area.setCaretPosition(0); }
    }

    Main() {
        super("CPU Scheduling Algorithm Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JButton addBtn = new JButton("Add Process"), clearBtn = new JButton("Clear All");
        JButton runBtn = new JButton("Run Simulation"), compareBtn = new JButton("Compare All");
        JPanel row1 = new JPanel(), row2 = new JPanel(), north = new JPanel(new GridLayout(2, 1)), center = new JPanel(new GridLayout(2, 1));
        row1.add(new JLabel("Process ID:")); row1.add(idBox); row1.add(new JLabel("AT:")); row1.add(atBox);
        row1.add(new JLabel("BT:")); row1.add(btBox); row1.add(new JLabel("Priority:")); row1.add(prBox);
        row1.add(addBtn); row1.add(clearBtn);
        row2.add(new JLabel("Algorithm:")); row2.add(algoBox); row2.add(new JLabel("Time Quantum:")); row2.add(qBox);
        row2.add(runBtn); row2.add(compareBtn);
        north.add(row1); north.add(row2);
        JTable inTable = new JTable(model);
        inTable.setDefaultEditor(Object.class, null);
        area.setEditable(false); area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        center.add(new JScrollPane(inTable)); center.add(new JScrollPane(area));
        add(north, BorderLayout.NORTH); add(center, BorderLayout.CENTER);
        addBtn.addActionListener(e -> addProcess());
        clearBtn.addActionListener(e -> { list.clear(); model.setRowCount(0); area.setText(""); });
        runBtn.addActionListener(e -> simulate(false));
        compareBtn.addActionListener(e -> simulate(true));
        setSize(820, 650); setLocationRelativeTo(null);
    }

    public static void main(String[] args) { new Main().setVisible(true); }
}
