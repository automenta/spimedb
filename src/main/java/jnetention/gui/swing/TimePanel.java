/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jnetention.gui.swing;

//import de.jaret.util.date.Interval;
//import de.jaret.util.date.IntervalImpl;
//import de.jaret.util.date.JaretDate;
//import de.jaret.util.ui.timebars.TimeBarMarker;
//import de.jaret.util.ui.timebars.TimeBarMarkerImpl;
//import de.jaret.util.ui.timebars.TimeBarViewerDelegate;
//import de.jaret.util.ui.timebars.TimeBarViewerInterface;
//import de.jaret.util.ui.timebars.mod.DefaultIntervalModificator;
//import de.jaret.util.ui.timebars.model.DefaultRowHeader;
//import de.jaret.util.ui.timebars.model.DefaultTimeBarModel;
//import de.jaret.util.ui.timebars.model.DefaultTimeBarRowModel;
//import de.jaret.util.ui.timebars.model.TimeBarModel;
//import de.jaret.util.ui.timebars.model.TimeBarRow;
//import de.jaret.util.ui.timebars.swing.TimeBarViewer;
//import de.jaret.util.ui.timebars.swing.renderer.BoxTimeScaleRenderer;
//import de.jaret.util.ui.timebars.swing.renderer.DefaultGapRenderer;
//import de.jaret.util.ui.timebars.swing.renderer.DefaultTimeScaleRenderer;
//import de.jaret.util.ui.timebars.swing.renderer.IMarkerRenderer;
//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.awt.Component;
//import java.awt.Graphics;
//import java.awt.GridLayout;
//import java.awt.datatransfer.StringSelection;
//import java.awt.dnd.DnDConstants;
//import java.awt.dnd.DragGestureEvent;
//import java.awt.dnd.DragGestureListener;
//import java.awt.dnd.DragGestureRecognizer;
//import java.awt.dnd.DragSource;
//import java.awt.dnd.DropTarget;
//import java.awt.dnd.DropTargetDragEvent;
//import java.awt.dnd.DropTargetDropEvent;
//import java.awt.dnd.DropTargetEvent;
//import java.awt.dnd.DropTargetListener;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.TooManyListenersException;
//import javax.swing.JCheckBox;
//import javax.swing.JComboBox;
//import javax.swing.JFrame;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.JSlider;
//import javax.swing.event.ChangeEvent;
//import javax.swing.event.ChangeListener;
//
///**
// *
// * @author me
// */
//public class TimePanel extends JPanel {
//
//    private final TimeBarViewer timebar;
//    private int rowsVisible = 3;
//
//    /** for testing */
//    public static void main(String[] args) {
//        JFrame f = new JFrame("");
//        f.setSize(1200, 800);
//        f.getContentPane().setLayout(new BorderLayout());
//        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//
//        TimePanel panel = new TimePanel();
//        f.getContentPane().add(panel, BorderLayout.CENTER);
//
//        f.setVisible(true);
//
//    }
//
//    public static class OtherIntervalImpl extends IntervalImpl {
//
//        String _label;
//
//        public OtherIntervalImpl() {
//            super();
//        }
//
//        public OtherIntervalImpl(JaretDate begin, JaretDate end) {
//            super(begin, end);
//        }
//
//        public void setLabel(String label) {
//            _label = label;
//        }
//
//        @Override
//        public String toString() {
//            return _label != null ? _label : super.toString();
//        }
//    }
//
//    public class OverlapControlPanel extends JPanel {
//
//        TimeBarViewer _viewer;
//
//        public OverlapControlPanel(TimeBarViewer viewer) {
//            _viewer = viewer;
//            setLayout(new GridLayout(5, 7));
//            createControls();
//        }
//
//        /**
//         *
//         */
//        private void createControls() {
//            JLabel label = new JLabel("pps");
//            add(label);
//            final JSlider timeScaleSlider = new JSlider(500, 5500);
//            timeScaleSlider.setValue((int) (_viewer.getPixelPerSecond() * 60.0 * 60.0 * 24));
//            timeScaleSlider.addChangeListener(new ChangeListener() {
//                public void stateChanged(ChangeEvent e) {
//                    double pixPerSecond = (double) timeScaleSlider.getValue() / (24.0 * 60 * 60);
//                    _viewer.setPixelPerSecond(pixPerSecond);
//                }
//            });
//            add(timeScaleSlider);
//
//            label = new JLabel("rowHeight");
//            add(label);
//
//            final JSlider rowHeigthSlider = new JSlider(10, 300);
//            rowHeigthSlider.setValue(_viewer.getRowHeight());
//            rowHeigthSlider.addChangeListener(new ChangeListener() {
//                public void stateChanged(ChangeEvent e) {
//                    _viewer.setRowHeight(rowHeigthSlider.getValue());
//                }
//            });
//            add(rowHeigthSlider);
//
//            final JCheckBox gapCheck = new JCheckBox("GapRenderer");
//            gapCheck.setSelected(_viewer.getGapRenderer() != null);
//            gapCheck.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    if (gapCheck.isSelected()) {
//                        _viewer.setGapRenderer(new DefaultGapRenderer());
//                    } else {
//                        _viewer.setGapRenderer(null);
//                    }
//                }
//            });
//            add(gapCheck);
//
//            final JCheckBox optScrollingCheck = new JCheckBox("Optimize scrolling");
//            optScrollingCheck.setSelected(_viewer.getOptimizeScrolling());
//            optScrollingCheck.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    _viewer.setOptimizeScrolling(optScrollingCheck.isSelected());
//                }
//            });
//            add(optScrollingCheck);
//
//            label = new JLabel("time scale position");
//            add(label);
//            final JComboBox timeScalePosCombo = new JComboBox();
//            timeScalePosCombo.addItem("top");
//            timeScalePosCombo.addItem("bottom");
//            timeScalePosCombo.addItem("none");
//            timeScalePosCombo.addActionListener(new ActionListener() {
//
//                public void actionPerformed(ActionEvent e) {
//                    if (timeScalePosCombo.getSelectedItem().equals("top")) {
//                        _viewer.setTimeScalePosition(TimeBarViewerInterface.TIMESCALE_POSITION_TOP);
//                    } else if (timeScalePosCombo.getSelectedItem().equals("bottom")) {
//                        _viewer.setTimeScalePosition(TimeBarViewerInterface.TIMESCALE_POSITION_BOTTOM);
//                    } else if (timeScalePosCombo.getSelectedItem().equals("none")) {
//                        _viewer.setTimeScalePosition(TimeBarViewerInterface.TIMESCALE_POSITION_NONE);
//                    }
//                }
//
//            });
//            add(timeScalePosCombo);
//
//            label = new JLabel("orientation");
//            add(label);
//            final JComboBox orientationCombo = new JComboBox();
//            orientationCombo.addItem("horizontal");
//            orientationCombo.addItem("vertical");
//            orientationCombo.addActionListener(new ActionListener() {
//
//                public void actionPerformed(ActionEvent e) {
//                    if (orientationCombo.getSelectedItem().equals("horizontal")) {
//                        _viewer.setTBOrientation(TimeBarViewerInterface.Orientation.HORIZONTAL);
//                    } else if (orientationCombo.getSelectedItem().equals("vertical")) {
//                        _viewer.setTBOrientation(TimeBarViewerInterface.Orientation.VERTICAL);
//                    }
//                }
//            });
//            add(orientationCombo);
//
//            final JCheckBox boxTSRCheck = new JCheckBox("BoxTimeScaleRenderer");
//            boxTSRCheck.setSelected(_viewer.getTimeScaleRenderer() instanceof BoxTimeScaleRenderer);
//            boxTSRCheck.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    _viewer.setTimeScaleRenderer(boxTSRCheck.isSelected() ? new BoxTimeScaleRenderer() : new DefaultTimeScaleRenderer());
//                }
//            });
//            add(boxTSRCheck);
//
//            final JCheckBox boxVRHCheck = new JCheckBox("Variable row height + dragging");
//            boxVRHCheck.setSelected(_viewer.getTimeBarViewState().getUseVariableRowHeights());
//            boxVRHCheck.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    _viewer.getTimeBarViewState().setUseVariableRowHeights(boxVRHCheck.isSelected());
//                    _viewer.setRowHeightDraggingAllowed(boxVRHCheck.isSelected());
//                }
//            });
//            add(boxVRHCheck);
//
//        }
//
//    }
//
//    public class CustomTimeBarMarker extends TimeBarMarkerImpl {
//
//        protected TimeBarRow _stopRow;
//
//        public CustomTimeBarMarker(boolean draggable, JaretDate date) {
//            super(draggable, date);
//        }
//
//        public CustomTimeBarMarker(boolean draggable, JaretDate date, TimeBarRow stopRow) {
//            super(draggable, date);
//            _stopRow = stopRow;
//        }
//
//        public TimeBarRow getStopRow() {
//            return _stopRow;
//        }
//
//    }
//
//    /**
//     * Stopping marker renderer rendering the marker as a single line.
//     * ATTENTION: does not support VERTICAL ORIENTATION
//     *
//     * @author kliem
//     * @version $Id: DefaultMarkerRenderer.java 823 2009-02-04 21:20:58Z kliem $
//     */
//    public class StoppingMarkerRenderer implements IMarkerRenderer {
//
//        /**
//         * color used when the marker is dragged.
//         */
//        protected Color _draggedColor = Color.GREEN;
//        /**
//         * color for marker rendering.
//         */
//        protected Color _markerColor = Color.RED;
//
//        /**
//         * {@inheritDoc}
//         */
//        public int getMarkerWidth(TimeBarMarker marker) {
//            return 4;
//        }
//
//        /**
//         * {@inheritDoc}
//         */
//        public void renderMarker(TimeBarViewerDelegate delegate, Graphics graphics, TimeBarMarker marker, int x,
//                boolean isDragged) {
//            Color oldCol = graphics.getColor();
//            if (isDragged) {
//                graphics.setColor(_draggedColor);
//            } else {
//                graphics.setColor(_markerColor);
//            }
//            int stopY;
//
//            if (marker instanceof CustomTimeBarMarker) {
//                CustomTimeBarMarker cmarker = (CustomTimeBarMarker) marker;
//                TimeBarRow stopRow = cmarker.getStopRow();
//                System.out.println("row " + stopRow);
//                if (delegate.isRowDisplayed(stopRow)) {
//                    stopY = delegate.getRowBounds(stopRow).y + delegate.getRowBounds(stopRow).height;
//                    System.out.println("found " + stopY);
//                } else {
//                    // might be above or below the viewport
//                    if (delegate.getAbsPosForRow(delegate.getRowIndex(stopRow)) < 0) {
//                        stopY = 0;
//                    } else {
//                        stopY = delegate.getDiagramRect().height + delegate.getXAxisHeight();
//                    }
//                }
//            } else {
//                // default behaviour
//                stopY = delegate.getDiagramRect().height + delegate.getXAxisHeight();
//            }
//
//            graphics.drawLine(x, 0, x, stopY);
//
//            graphics.setColor(oldCol);
//        }
//
//    }
//
//    public TimePanel() {
//        super(new BorderLayout());
//        
//        final TimeBarModel model = ModelCreator.createModel();
//        timebar = new TimeBarViewer(model);
//
//        timebar.setAutoScaleRows(rowsVisible);
//        timebar.addIntervalModificator(new DefaultIntervalModificator());
//
//        timebar.setPixelPerSecond(0.05);
//        timebar.setDrawRowGrid(true);
//
//        timebar.setDrawOverlapping(false);
//        timebar.setSelectionDelta(6);
//        timebar.setTimeScalePosition(TimeBarViewerInterface.TIMESCALE_POSITION_TOP);
//        timebar.setTBOrientation(TimeBarViewerInterface.Orientation.VERTICAL);
//        timebar.setAllowMouseWheelXAxisScrolling(false);
//        timebar.setOptimizeScrolling(false);
//
//        // timebar marker
//        CustomTimeBarMarker marker1 = new CustomTimeBarMarker(true, new JaretDate().advanceHours(1), model.getRow(2));
//        timebar.addMarker(marker1);
//        //CustomTimeBarMarker marker2 = new CustomTimeBarMarker(true, new JaretDate().advanceHours(2), model.getRow(4));
//        //timebar.addMarker(marker2);
//        
//        //timebar.setMarkerRenderer(new StoppingMarkerRenderer());
//
//        /*
//        timebar.setGlobalAssistantRenderer(new IGlobalAssistantRenderer() {
//
//            @Override
//            public void doRenderingLast(TimeBarViewerDelegate delegate, Graphics graphics) {
//                TimeBarRow row = delegate.getRow(2); // just use row ad index 2 as a test
//                Rectangle bounds = delegate.getRowBounds(row);
//                int endX = delegate.xForDate(new JaretDate().advanceHours(3)); // end date 
//                Graphics2D g2 = (Graphics2D) graphics;
//
//                g2.setPaint(new GradientPaint(endX - 300, bounds.y, Color.WHITE, endX, bounds.y, Color.GREEN));
//                float alpha = .3f;
//                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
//                g2.fillRect(endX - 300, bounds.y, 300, bounds.height); // just draw 300 pixels from dest x
//            }
//
//            @Override
//            public void doRenderingBeforeIntervals(TimeBarViewerDelegate delegate, Graphics graphics) {
//            }
//        });
//        */
//
//        // Box tsr with DST correction
//        // BoxTimeScaleRenderer btsr = new BoxTimeScaleRenderer();
//        // btsr.setCorrectDST(true);
//        // _tbv.setTimeScaleRenderer(btsr);
//        add(timebar, BorderLayout.CENTER);
//
//        //add(new OverlapControlPanel(timebar), BorderLayout.SOUTH);
//
//
//        setupDND();
//        
//    }
//
//    private ArrayList<Interval> _draggedJobs;
//    private ArrayList<Integer> _draggedJobsOffsets;
//    private DefaultTimeBarRowModel _tbvDragOrigRow;
//    private JaretDate _tbvDragOrigBegin;
//    private JaretDate _tbvDragOrigEnd;
//
//    /**
//     * Setup time bar viewer as drag source and drop target. Quick hack: - one
//     * interval only - hold ALT to start a drag (could be differentiated by
//     * other means)
//     */
//    private void setupDND() {
//
//        // Drag source
//        DragSource dragSource = DragSource.getDefaultDragSource();
//        DragGestureListener dgl = new TimeBarViewerDragGestureListener();
//        DragGestureRecognizer dgr = dragSource.createDefaultDragGestureRecognizer(timebar._diagram,
//                DnDConstants.ACTION_MOVE, dgl);
//
//        // create and setup drop target
//        DropTarget dropTarget = new DropTarget();
//        timebar.setDropTarget(dropTarget);
//
//        try {
//            dropTarget.addDropTargetListener(new DropTargetListener() {
//
//                public void dropActionChanged(DropTargetDragEvent evt) {
//                }
//
//                public void drop(DropTargetDropEvent evt) {
//                    if (_draggedJobs != null) {
//                        TimeBarRow overRow = timebar.getRowForXY(evt.getLocation().x, evt.getLocation().y);
//                        if (overRow != null) {
//                            for (Interval job : _draggedJobs) {
//                                ((DefaultTimeBarRowModel) overRow).addInterval(job);
//                            }
//                            timebar.setGhostIntervals(null, null);
//                            evt.dropComplete(true);
//                            evt.getDropTargetContext().dropComplete(true);
//                            // TODO mystic problem with drop success
//                            _tbvDragOrigRow = null; // mark the drag successful ...
//                        }
//                        timebar.deHighlightRow();
//                    }
//                }
//
//                public void dragOver(DropTargetDragEvent evt) {
//                    TimeBarRow overRow = timebar.getRowForXY(evt.getLocation().x, evt.getLocation().y);
//                    if (overRow != null) {
//                        timebar.highlightRow(overRow);
//
//                        JaretDate curDate = timebar.dateForXY(evt.getLocation().x, evt.getLocation().y);
//                        correctDates(_draggedJobs, curDate);
//
//                        // tell the timebar viewer
//                        timebar.setGhostIntervals(_draggedJobs, _draggedJobsOffsets);
//                        timebar.setGhostOrigin(evt.getLocation().x, evt.getLocation().y);
//                        // there could be a check whether dropping is allowed at the current location
//                        if (true) {// dropAllowed(_draggedJobs, overRow)) {
//                            evt.acceptDrag(DnDConstants.ACTION_MOVE);
//                        } else {
//                            evt.rejectDrag();
//                            timebar.setGhostIntervals(null, null);
//                        }
//                    } else {
//                        timebar.deHighlightRow();
//                    }
//                }
//
//                public void dragExit(DropTargetEvent evt) {
//                    timebar.deHighlightRow();
//                }
//
//                public void dragEnter(DropTargetDragEvent evt) {
//                }
//            });
//        } catch (TooManyListenersException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }
//
//    private void correctDates(List<Interval> draggedJobs, JaretDate curDate) {
//        for (int i = 0; i < draggedJobs.size(); i++) {
//            Interval interval = draggedJobs.get(i);
//            int secs = interval.getSeconds();
//            interval.setBegin(curDate.copy());
//            interval.setEnd(curDate.copy().advanceSeconds(secs));
//        }
//    }
//
//    /**
//     * Drag gesture listener.
//     *
//     * @author kliem
//     * @version $id:$
//     */
//    class TimeBarViewerDragGestureListener implements DragGestureListener {
//
//        public void dragGestureRecognized(DragGestureEvent e) {
//            Component c = e.getComponent();
//
//            // if a marker is being dragged -> do nothing
//            boolean markerDragging = timebar.getDelegate().isMarkerDraggingInProgress();
//            if (markerDragging) {
//                return;
//            }
//
//            // check the intervals and maybe start a drag
//            List<Interval> intervals = timebar.getDelegate().getIntervalsAt(e.getDragOrigin().x, e.getDragOrigin().y);
//            // start drag only if ALT is pressed
//            if (intervals.size() > 0 && e.getTriggerEvent().isAltDown()) {
//                    try {
//                    Interval interval = intervals.get(0);
//                    e.startDrag(null, new StringSelection("Drag " + interval));
//                    _draggedJobs = new ArrayList<Interval>();
//                    _draggedJobs.add(interval);
//                    _draggedJobsOffsets = new ArrayList<Integer>();
//                    _draggedJobsOffsets.add(0);
//                    TimeBarRow row = timebar.getModel().getRowForInterval(interval);
//                    ((DefaultTimeBarRowModel) row).remInterval(interval);
//
//                    // save orig data
//                    _tbvDragOrigRow = (DefaultTimeBarRowModel) row;
//                    _tbvDragOrigBegin = interval.getBegin().copy();
//                    _tbvDragOrigEnd = interval.getEnd().copy();
//                    }
//                    catch (Exception re) { }
//
//                return;
//            }
//        }
//    }
//
//    public static class ModelCreator {
//
//        public static TimeBarModel createModel() {
//            DefaultTimeBarModel model = new DefaultTimeBarModel();
//
//            JaretDate date = new JaretDate();
//
//            int length = 120;
//
//            DefaultRowHeader header = new DefaultRowHeader("r1");
//            DefaultTimeBarRowModel tbr = new DefaultTimeBarRowModel(header);
//            IntervalImpl interval = new IntervalImpl();
//            interval.setBegin(date.copy());
//            interval.setEnd(date.copy().advanceMinutes(length));
//            tbr.addInterval(interval);
//
//            interval = new IntervalImpl();
//            interval.setBegin(date.copy().advanceMinutes(30));
//            interval.setEnd(date.copy().advanceMinutes(length));
//            tbr.addInterval(interval);
//
//            interval = new IntervalImpl();
//            interval.setBegin(date.copy().advanceMinutes(60));
//            interval.setEnd(interval.getBegin().copy().advanceMinutes(length));
//            tbr.addInterval(interval);
//
//            // very short interval
//            interval = new OtherIntervalImpl();
//            interval.setBegin(date.copy().advanceMinutes(60 + 120 + 10));
//            interval.setEnd(interval.getBegin().copy().advanceMillis(20));
//            tbr.addInterval(interval);
//
//            model.addRow(tbr);
//
//            header = new DefaultRowHeader("r2");
//            tbr = new DefaultTimeBarRowModel(header);            
//            interval = new IntervalImpl();
//            interval.setBegin(date.copy());
//            interval.setEnd(date.copy().advanceMinutes(length));
//            tbr.addInterval(interval);
//
//            interval = new IntervalImpl();
//            interval.setBegin(date.copy().advanceMinutes(120));
//            interval.setEnd(date.copy().advanceMinutes(length + length));
//            tbr.addInterval(interval);
//
//            model.addRow(tbr);
//
//            header = new DefaultRowHeader("r3");
//            tbr = new DefaultTimeBarRowModel(header);
//            interval = new OtherIntervalImpl();
//            interval.setBegin(date.copy());
//            interval.setEnd(date.copy().advanceMinutes(length));
//            tbr.addInterval(interval);
//
//            interval = new OtherIntervalImpl();
//            interval.setBegin(date.copy().advanceMinutes(30));
//            interval.setEnd(date.copy().advanceMinutes(length));
//            tbr.addInterval(interval);
//
//            interval = new OtherIntervalImpl();
//            interval.setBegin(date.copy().advanceMinutes(60));
//            interval.setEnd(interval.getBegin().copy().advanceMinutes(length));
//            tbr.addInterval(interval);
//
//            interval = new OtherIntervalImpl();
//            interval.setBegin(date.copy().advanceMinutes(90));
//            interval.setEnd(interval.getBegin().copy().advanceMinutes(length));
//            tbr.addInterval(interval);
//
//            model.addRow(tbr);
//
//        // create a row with intervals hitting the dst dates (germany)
////        
////        header = new DefaultRowHeader("DST");
////        tbr = new DefaultTimeBarRowModel(header);
////
////        
////        interval = new OtherIntervalImpl();
////        interval.setBegin(new JaretDate(28, 3, 2009, 23, 0, 0));
////        interval.setEnd(interval.getBegin().copy().advanceMinutes(120));
////        tbr.addInterval(interval);
////
////        interval = new OtherIntervalImpl();
////        interval.setBegin(new JaretDate(29, 3, 2009, 3, 0, 0));
////        interval.setEnd(interval.getBegin().copy().advanceMinutes(120));
////        tbr.addInterval(interval);
////
////        
////        interval = new OtherIntervalImpl();
////        interval.setBegin(new JaretDate(24, 10, 2009, 23,0,0 ));
////        interval.setEnd(interval.getBegin().copy().advanceMinutes(120));
////        tbr.addInterval(interval);
////
////        interval = new OtherIntervalImpl();
////        interval.setBegin(new JaretDate(24,10,2009, 23, 0, 0));
////        interval.setEnd(interval.getBegin().copy().advanceMinutes(180));
////        tbr.addInterval(interval);
////
////        interval = new OtherIntervalImpl();
////        interval.setBegin(new JaretDate(25, 10, 2009, 2, 0, 0));
////        interval.setEnd(interval.getBegin().copy().advanceMinutes(120));
////        tbr.addInterval(interval);
////
////        interval = new OtherIntervalImpl();
////        interval.setBegin(new JaretDate(25, 10, 2009, 3, 0, 0));
////        interval.setEnd(interval.getBegin().copy().advanceMinutes(120));
////        tbr.addInterval(interval);
////        
////        model.addRow(tbr);
//            /*
//            // add some empty rows for drag&drop fun
//            for (int rowNumber = 4; rowNumber <= 20; rowNumber++) {
//                header = new DefaultRowHeader("r" + rowNumber);
//                tbr = new DefaultTimeBarRowModel(header);
//                model.addRow(tbr);
//
//            }
//                    */
//
//            return model;
//        }
//
//        public static TimeBarModel createLargeModel() {
//            DefaultTimeBarModel model = new DefaultTimeBarModel();
//
//            JaretDate date = new JaretDate();
//
//            int length = 120;
//            double delta = 5.0;
//
//            int rows = 6;
//            int intervals = 10;
//
//            for (int r = 0; r < rows; r++) {
//                DefaultRowHeader header = new DefaultRowHeader("r" + r);
//                DefaultTimeBarRowModel tbr = new DefaultTimeBarRowModel(header);
//                for (int i = 0; i < intervals; i++) {
//                    IntervalImpl interval = new IntervalImpl();
//                    interval.setBegin(date.copy().advanceMinutes(Math.random() * delta));
//                    interval.setEnd(interval.getBegin().copy().advanceMinutes(length));
//                    tbr.addInterval(interval);
//                }
//                model.addRow(tbr);
//
//            }
//
//            return model;
//        }
//
//    }
//}
