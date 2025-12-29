package View.CustomizeCorrelationPage;

import Entity.CheckableItem;
import Entity.Request;
import Services.CustomizeCorrelationView.ButtonsAction;
import Services.CustomizeCorrelationView.CorrelatorHelperService;
import Services.CustomizeCorrelationView.CustomCorrelationFrameService;
import Services.CustomizeCorrelationView.JTableAction;
import View.AddManuallyCorrelationPage.UsePreviouseResponseModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CustomizeCorrelationPage extends JFrame {


    public void setCorrelatorHelperApp(CorrelatorHelperService correlatorHelperService) {
        this.correlatorHelperService = correlatorHelperService;
    }

    public void setUrl() {
        Request request= this.correlatorHelperService.getDependencyGraph().nodes.get(current_request).getRequest();
        String url_s = request.getUrl();
        String method = request.getMethod();
        String indexs = this.correlatorHelperService.getDependencyGraph().nodes.get(current_request).toString();
        url.setText("<html> <b>"+current_request+"/"+(correlatorHelperService.getDependencyGraph().nodes.size()-1)+"</b> " +
                "<b> ["+method+"]</b> "+url_s+" </html>");
    }

    public void enableTabByUrl () {
        if(!( this.correlatorHelperService.getDependencyGraph().nodes.get(current_request).request.getMethod().equals("POST")||(this.correlatorHelperService.getDependencyGraph().nodes.get(current_request).request.getMethod().equals("PUT")))){
            tabbedPane1.setEnabledAt(2,false);
        }else{
            tabbedPane1.setEnabledAt(2,true);
        }
        if(!this.correlatorHelperService.getDependencyGraph().nodes.get(current_request).getRequest().getUrl().startsWith("wss") &&
                !this.correlatorHelperService.getDependencyGraph().nodes.get(current_request).getRequest().getUrl().startsWith(("ws"))
        ){
            //tabbedPane1.setEnabledAt(4,false);
            tabbedPane1.setEnabledAt(5,false);
        } else {
            //tabbedPane1.setEnabledAt(4,true);
            tabbedPane1.setEnabledAt(5,true);
        }
    }

    public void setActionTableComponent() throws IOException {

        headers.getAdd().addActionListener(ButtonsAction.actionCustomCorrelationAdd(this,"headers"));
        headers.getPreviousResponsePanel().setVisible(false);
        query_parameters.getAdd().addActionListener(ButtonsAction.actionCustomCorrelationAdd(this,"query"));
        query_parameters.getPreviousResponsePanel().setVisible(false);

        replace_url.getAdd().addActionListener(ButtonsAction.actionCustomCorrelationAdd(this,"url"));
        replace_url.getPreviousResponsePanel().setVisible(false);

        cookie.getAdd().addActionListener(ButtonsAction.actionCustomCorrelationAdd(this,"cookie"));
        cookie.getPreviousResponsePanel().setVisible(false);

        //request_ws.getAdd().addActionListener(ButtonsAction.actionCustomCorrelationAdd(this,"request"));
        //request_ws.getPreviousResponsePanel().setVisible(false);

        postData.getAdd().addActionListener(ButtonsAction.actionCustomCorrelationAdd(this,"postData"));
        //from combo box (the same for all)
        List<String> list_from = null;

        list_from = CustomCorrelationFrameService.getUrls(
                this.getCorrelatorHelperApp().getDependencyGraph(),
                this.getCurrent_request()
        );
    /*
        postData.getPreviousResponseComboBox().setModel( new DefaultComboBoxModel(list_from.toArray()));
        postData.getPreviousResponseCheckBox().addActionListener(ButtonsAction.actionSetPreviousResponsePostDataCheckBox(this));
        postData.getPreviousResponseComboBox().addActionListener(ButtonsAction.actionSetPreviousResponsePostDataComboBox(this));

        headers.getUncheck().addActionListener(ButtonsAction.actionUnchekAllTableComponent(this,headers,"headers"));
        query_parameters.getUncheck().addActionListener(ButtonsAction.actionUnchekAllTableComponent(this,query_parameters,"query"));
        replace_url.getUncheck().addActionListener(ButtonsAction.actionUnchekAllTableComponent(this,replace_url,"url"));
        cookie.getUncheck().addActionListener(ButtonsAction.actionUnchekAllTableComponent(this,cookie,"cookie"));
        request_ws.getUncheck().addActionListener(ButtonsAction.actionUnchekAllTableComponent(this,request_ws,"request"));
        postData.getUncheck().addActionListener(ButtonsAction.actionUnchekAllTableComponent(this,postData,"postData"));
    */
    }

    public void setReplacementTableModel () {
        replacementTableModelHeaders = new ReplacementTableModel(
                ReplacementTableModel.TableView.HEADER,
                this.checkItemListsRequest.get(current_request).get("headers")
        );

        headers.setReplacementTableModel(replacementTableModelHeaders);
        headers.getTable().getColumnModel().getColumn(0).setMaxWidth(30);
        headers.getTable().getColumnModel().getColumn(5).setMaxWidth(30);

        JTableAction.setActionJTable(headers.getTable(),this,"headers");

        replacementTableModelQuery = new ReplacementTableModel(
                ReplacementTableModel.TableView.QUERY,
                this.checkItemListsRequest.get(current_request).get("query")
        );

        query_parameters.setReplacementTableModel(replacementTableModelQuery);
        query_parameters.getTable().getColumnModel().getColumn(0).setMaxWidth(30);
        query_parameters.getTable().getColumnModel().getColumn(5).setMaxWidth(30);
        JTableAction.setActionJTable(query_parameters.getTable(),this,"query");

        replacementTableModelUrl = new ReplacementTableModel(
                ReplacementTableModel.TableView.URL,
                this.checkItemListsRequest.get(current_request).get("url")
        );

        replace_url.setReplacementTableModel(replacementTableModelUrl);
        replace_url.getTable().getColumnModel().getColumn(0).setMaxWidth(30);
        replace_url.getTable().getColumnModel().getColumn(5).setMaxWidth(30);
        JTableAction.setActionJTable(replace_url.getTable(),this,"url");

        replacementTableModelCookie = new ReplacementTableModel(
                ReplacementTableModel.TableView.COOKIE,
                this.checkItemListsRequest.get(current_request).get("cookie")
        );
        cookie.setReplacementTableModel(replacementTableModelCookie);
        cookie.getTable().getColumnModel().getColumn(0).setMaxWidth(30);
        cookie.getTable().getColumnModel().getColumn(5).setMaxWidth(30);
        JTableAction.setActionJTable(cookie.getTable(),this,"cookie");

        /*
        replacementTableModelRequest = new ReplacementTableModel(
                ReplacementTableModel.TableView.REQUEST,
                this.checkItemListsRequest.get(current_request).get("request")
        );
        request_ws.setReplacementTableModel(replacementTableModelRequest);
        request_ws.getTable().getColumnModel().getColumn(0).setMaxWidth(30);
        request_ws.getTable().getColumnModel().getColumn(7).setMaxWidth(30);
        JTableAction.setActionJTable(request_ws.getTable(),this,"request");
        */

        replacementTablePostData = new ReplacementTableModel(
                ReplacementTableModel.TableView.POSTDATA,
                this.checkItemListsRequest.get(current_request).get("postData")
        );
        postData.setReplacementTableModel(replacementTablePostData);
        postData.getTable().getColumnModel().getColumn(0).setMaxWidth(30);
        postData.getTable().getColumnModel().getColumn(5).setMaxWidth(30);
        JTableAction.setActionJTable(postData.getTable(),this,"postData");
    }

    public void setActionSx(ActionListener actionListener) {
        sx.addActionListener(actionListener);
    }


    public void setActionDx(ActionListener actionListener) {
        dx.addActionListener(actionListener);
    }
    public void setActionSave(ActionListener actionListener) {save.addActionListener(actionListener);}

    public CustomizeCorrelationPage() { initComponents(); }

    public TableComponent getPostData() {
        return postData;
    }

    private void initComponents() {

        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panel1 = new JPanel();
        panel2 = new JPanel();
        sx = new JButton();
        dx = new JButton();
        url = new JLabel();
        panel6 = new JPanel();
        infoRequestsLabel = new JLabel();
        infoRequestsButton = new JButton();
        panel7 = new JPanel();
        label2 = new JLabel();
        tabbedPane1 = new JTabbedPane();
        headers = new TableComponent();
        query_parameters = new TableComponent();
        postData = new TableComponent();
        replace_url = new TableComponent();
        cookie = new TableComponent();
        request_ws = new TableComponent();
        panel3 = new JPanel();
        save = new JButton();
        panel4 = new JPanel();
        label1 = new JLabel();
        panel5 = new JPanel();
        twoPartNameE2E = new JTextField();
        nameE2E = new JLabel();

        //======== this ========
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(5, 0));

        //======== panel1 ========
        {
            panel1.setLayout(new BorderLayout(11, 10));

            //======== panel2 ========
            {
                panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));

                //---- sx ----
                //sx.setText("previous");
                panel2.add(sx);

                //---- dx ----
                //dx.setText("next");
                panel2.add(dx);
            }
            panel1.add(panel2, BorderLayout.EAST);

            //---- url ----
            url.setText("text");
            url.setFont(new Font("Droid Sans Mono", Font.PLAIN, 15));
            url.setHorizontalAlignment(SwingConstants.LEFT);
            panel1.add(url, BorderLayout.CENTER);

            //======== panel6 ========
            {
                panel6.setLayout(new BorderLayout(5, 5));

                //---- infoRequestsLabel ----
                infoRequestsLabel.setText("text");
                infoRequestsLabel.setFont(new Font("Cantarell", Font.BOLD, 12));
                panel6.add(infoRequestsLabel, BorderLayout.CENTER);

                //---- infoRequestsButton ----
                //infoRequestsButton.setText("info");
                panel6.add(infoRequestsButton, BorderLayout.WEST);
            }
            panel1.add(panel6, BorderLayout.NORTH);

            //======== panel7 ========
            {
                panel7.setLayout(new BorderLayout(10, 10));

                //---- label2 ----
                label2.setText("CORRELATIONS FOUND");
                label2.setHorizontalAlignment(SwingConstants.CENTER);
                label2.setFont(label2.getFont().deriveFont(label2.getFont().getStyle() | Font.BOLD));
                panel7.add(label2, BorderLayout.CENTER);
            }
            panel1.add(panel7, BorderLayout.SOUTH);
        }
        contentPane.add(panel1, BorderLayout.NORTH);

        //======== tabbedPane1 ========
        {
            tabbedPane1.addTab("HEADERS", headers);
            tabbedPane1.addTab("QUERY P.", query_parameters);
            tabbedPane1.addTab("POST DATA", postData);
            tabbedPane1.addTab("URL", replace_url);
            tabbedPane1.addTab("COOKIE", cookie);
            tabbedPane1.addTab("REQUEST WS", request_ws);
        }
        contentPane.add(tabbedPane1, BorderLayout.CENTER);

        //======== panel3 ========
        {
            panel3.setLayout(new BorderLayout(5, 5));

            //---- save ----
            save.setText("SAVE");
            save.setBackground(new Color(245,121,0));
            save.setForeground(new Color(255,255,255));
            panel3.add(save, BorderLayout.EAST);

            //======== panel4 ========
            {
                panel4.setLayout(new BorderLayout(5, 5));

                //---- label1 ----
                label1.setText(".json");
                panel4.add(label1, BorderLayout.EAST);

                //======== panel5 ========
                {
                    panel5.setLayout(new BorderLayout(5, 5));

                    //---- twoPartNameE2E ----
                    twoPartNameE2E.setMinimumSize(new Dimension(100, 30));
                    twoPartNameE2E.setMaximumSize(new Dimension(100, 30));
                    twoPartNameE2E.setPreferredSize(new Dimension(100, 30));
                    panel5.add(twoPartNameE2E, BorderLayout.CENTER);

                    //---- nameE2E ----
                    nameE2E.setText("text");
                    panel5.add(nameE2E, BorderLayout.WEST);
                }
                panel4.add(panel5, BorderLayout.CENTER);
            }
            panel3.add(panel4, BorderLayout.CENTER);
        }
        contentPane.add(panel3, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panel1;
    private JPanel panel2;
    private JButton sx;
    private JButton dx;
    private JLabel url;
    private JPanel panel6;
    private JLabel infoRequestsLabel;
    private JButton infoRequestsButton;
    private JPanel panel7;
    private JLabel label2;
    private JTabbedPane tabbedPane1;
    private TableComponent headers;
    private TableComponent query_parameters;
    private TableComponent postData;
    private TableComponent replace_url;
    private TableComponent cookie;
    private TableComponent request_ws;
    private JPanel panel3;
    private JButton save;
    private JPanel panel4;
    private JLabel label1;
    private JPanel panel5;
    private JTextField twoPartNameE2E;
    private JLabel nameE2E;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    public JLabel getNameE2E() {
        return nameE2E;
    }

    public void setNameE2E(JLabel nameE2E) {
        this.nameE2E = nameE2E;
    }

    public JTextField getTwoPartNameE2E() {
        return twoPartNameE2E;
    }

    public void setTwoPartNameE2E(JTextField twoPartNameE2E) {
        this.twoPartNameE2E = twoPartNameE2E;
    }
    public JButton getSx() {
        return sx;
    }

    public JButton getDx() {
        return dx;
    }

    public List<Map<String,List<CheckableItem>>> getCheckItemListsRequest() {
        return checkItemListsRequest;
    }

    public void setCheckItemListsRequest(List<Map<String, List<CheckableItem>>> checkItemListsRequest) {
        this.checkItemListsRequest = checkItemListsRequest;
    }

    private List<Map<String, List<CheckableItem>>> checkItemListsRequest;



    public CorrelatorHelperService getCorrelatorHelperApp() {
        return correlatorHelperService;
    }

    private CorrelatorHelperService correlatorHelperService;

    public ReplacementTableModel getReplacementTableModelHeaders() {
        return replacementTableModelHeaders;
    }

    public ReplacementTableModel getReplacementTableModelQuery() {
        return replacementTableModelQuery;
    }

    public ReplacementTableModel getReplacementTableModelUrl() {
        return replacementTableModelUrl;
    }

    private ReplacementTableModel replacementTableModelHeaders;
    private ReplacementTableModel replacementTableModelQuery;
    private ReplacementTableModel replacementTableModelUrl;

    public ReplacementTableModel getReplacementTableModelCookie() {
        return replacementTableModelCookie;
    }

    public ReplacementTableModel getReplacementTableModelRequest() {
        return replacementTableModelRequest;
    }

    private ReplacementTableModel replacementTableModelCookie;
    private ReplacementTableModel replacementTableModelRequest;

    public ReplacementTableModel getReplacementTablePostData() {
        return replacementTablePostData;
    }

    public void setReplacementTablePostData(ReplacementTableModel replacementTablePostData) {
        this.replacementTablePostData = replacementTablePostData;
    }

    private ReplacementTableModel replacementTablePostData;

    public int getCurrent_request() {
        return current_request;
    }

    public void setCurrent_request(int current_request) {
        this.current_request = current_request;
    }

    private int current_request = 0;

    public JButton getInfoRequestsButton() {
        return infoRequestsButton;
    }

    public JLabel getInfoRequestsLabel() {
        return infoRequestsLabel;
    }

    public String getPathE2ETests() {
        return pathE2ETests;
    }

    public void setPathE2ETests(String pathE2ETests) {
        this.pathE2ETests = pathE2ETests;
    }

    private String pathE2ETests ;

    public UsePreviouseResponseModel getUsePreviouseResponseModel() {
        return usePreviouseResponseModel;
    }

    public UsePreviouseResponseModel usePreviouseResponseModel = new UsePreviouseResponseModel();


}
