/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package featureextractor.plot;

import featureextractor.extractor.db.DbExtractor;
import featureextractor.model.Batch;
import featureextractor.model.DataTime;
import featureextractor.model.SingleCoordinateSet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author ark0n3
 */
public class Plot extends javax.swing.JFrame {

    public final static int time_divisor = 10000000;
    private long[] marker = new long[2];
    private List<IntervalMarker> markers = new ArrayList<IntervalMarker>();
    private int marker_idx = 0;
    private final DbExtractor db_extractor;
    private final Batch batch;

    private void addPlot() {
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setMinimumSize(new Dimension(1024, 768));
        JFreeChart chart = ChartFactory.createXYLineChart(
                batch.getTitle(),
                "Timestamp",
                "m/s^2",
                this.createDataset(batch),
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        chart.setBackgroundPaint(Color.LIGHT_GRAY);
        chart.addProgressListener(new ChartProgressListener() {
            @Override
            public void chartProgress(ChartProgressEvent e) {
                if (e.getSource() instanceof MouseEvent) System.out.println("mouse");
                long value=(long)e.getChart().getXYPlot().getDomainCrosshairValue();
                if (e.getType() == 2 && e.getPercent()==100 && value>0 && value!=marker[1]) {
                    
                    if (marker_idx>1) {
                        marker_idx=0;
                        marker=new long[2]; // reset marker range
                    }
                    marker[marker_idx]=value;
                    Plot.this.txtSelected.setText("Setting marker["+marker_idx+"]="+value);
                    if (marker_idx==1) {
                        markers.add(new IntervalMarker(marker[0], marker[1]));
                        e.getChart().getXYPlot().addDomainMarker(markers.get(markers.size()-1));
                    }
                    marker_idx++;
                }
            }
        });
        
        XYPlot xyplot = chart.getXYPlot();
        xyplot.setRangeCrosshairVisible(true);
        xyplot.setDomainCrosshairVisible(true);
        xyplot.setDomainCrosshairLockedOnData(false);
        xyplot.setBackgroundPaint(Color.white);
        xyplot.setRangePannable(true);
        xyplot.setDomainPannable(true);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);
        chartPanel.setMouseWheelEnabled(true);
        this.mainPanel.add(chartPanel, BorderLayout.CENTER);
        this.setVisible(true);
    }
    
    /**
     * Creates new form Plot
     */
    public Plot(Batch batch, DbExtractor db_extractor) {
        this.db_extractor=db_extractor;
        this.batch=batch;
        initComponents();
        addPlot();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JLabel getTxtSelected() {
        return txtSelected;
    }

    private XYDataset createDataset(Batch batch) {
        java.util.List<SingleCoordinateSet> axes = batch.getValues();
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (int axis = 0; axis < axes.size(); axis++) {
            XYSeries series = new XYSeries(axes.get(axis).getTitle());
            for (DataTime dt : axes.get(axis).getValues()) {
                series.add(dt.getTime() / time_divisor, dt.getValue());
            }
            dataset.addSeries(series);
        }
        return dataset;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        txtSelected = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setLayout(new java.awt.BorderLayout());
        jPanel1.add(txtSelected, java.awt.BorderLayout.PAGE_START);

        jButton2.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jButton2.setText("SAVE STAIRS");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton2, java.awt.BorderLayout.PAGE_END);

        mainPanel.setLayout(new java.awt.BorderLayout());

        jLabel1.setText("jLabel1");
        mainPanel.add(jLabel1, java.awt.BorderLayout.CENTER);

        jPanel1.add(mainPanel, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        try {
            if (markers.isEmpty()) throw new Exception("No marker set yet");
            if (batch.getTrunk()==0) throw new Exception("This batch is not a trunk");
            db_extractor.setSteps(markers, batch.getTrunk());
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "ERRORE", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JLabel txtSelected;
    // End of variables declaration//GEN-END:variables
}
