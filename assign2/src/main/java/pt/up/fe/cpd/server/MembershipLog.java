package pt.up.fe.cpd.server;

import java.util.List;
import java.util.stream.Collectors;
import java.util.LinkedList;

public class MembershipLog {
    private LinkedList<MembershipLogEntry> entries;

    public MembershipLog(){
        this.entries = new LinkedList<>();
    }

    public List<MembershipLogEntry> getEntries() {
        return entries;
    }

    public void addEntry(MembershipLogEntry entry) {
        if (entries.contains(entry)) return;

        if(this.entries.size() >= 32) {
            this.entries.removeFirst();
        }
        this.entries.add(entry);
    }

    @Override
    public String toString() {
        return entries.stream().map(e -> e.toString()).collect(Collectors.joining(", "));
    }
}
