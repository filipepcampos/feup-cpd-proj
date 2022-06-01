package pt.up.fe.cpd.server.membership.log;

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

    public boolean addEntry(MembershipLogEntry entry) {
        /*
        Merging Rules
        - Rule 1: If the event is already in the local log, skip it
        - Rule 2: If the event is older (you should use the membership count) than the event for that member in the local log, skip it
        - Rule 3: If the event is new, i.e. there is no event in the local log for that member, add that event to the tail of the log
            (assuming that events at the tail are the most recent)
        - Rule 4: If the event is newer than the event for that member in the local log, remove the event for that member from the local
            log, and add the newer event at the tail of the log (i.e. as if it was a new event)
        */
        
        if (entries.contains(entry)) return false;
        else {
            for (MembershipLogEntry compareEntry : entries) {
                if (entry.equals(compareEntry)) {
                    if (entry.getMembershipCounter() < compareEntry.getMembershipCounter()) return false;
                    if (entry.getMembershipCounter() > compareEntry.getMembershipCounter()) {
                        this.entries.remove(compareEntry);
                        this.entries.add(entry);
                        return true;
                    }
                }
            }
        }

        if (this.entries.size() >= 32) {
            this.entries.removeFirst();
        }
        this.entries.add(entry);
        return true;
    }

    @Override
    public String toString() {
        return entries.stream().map(e -> e.toString()).collect(Collectors.joining(", "));
    }
}
