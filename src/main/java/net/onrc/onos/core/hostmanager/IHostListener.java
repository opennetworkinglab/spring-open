package net.onrc.onos.core.hostmanager;

public interface IHostListener {

    public void hostAdded(Host host);

    public void hostRemoved(Host host);
}
