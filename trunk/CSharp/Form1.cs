using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;

namespace multi_http_send
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();
        }

        private void btnBrowser_Click(object sender, EventArgs e)
        {
            DialogResult result = this.openFileDialog1.ShowDialog();
            if (result == DialogResult.OK)
            {
                if(!this.fileList.Items.Contains(this.openFileDialog1.FileName))
                this.fileList.Items.Add(this.openFileDialog1.FileName);
            }
        }

        private void btnSubmit_Click(object sender_obj, EventArgs e)
        {

            MultiPartDataSender sender = new MultiPartDataSender();
            sender.RemoteURL = this.uploadURL.Text;
   
   
            DataGridViewRow row;
            for (int i = 0; i < this.cookieDataGrid.Rows.Count; i++)
            {

                 row = this.cookieDataGrid.Rows[i];
                if (row.Cells[0].Value != null && row.Cells[1].Value != null)
                {
                    sender.AddCookie(row.Cells[0].Value as string, row.Cells[1].Value as string);
                }
            }

            for (int i = 0; i < this.textFieldGrid.Rows.Count; i++)
            {
                row = this.textFieldGrid.Rows[i];

                if (row.Cells[0].Value != null && row.Cells[1].Value != null)
                {
                    sender.AddTextField(row.Cells[0].Value as string, row.Cells[1].Value as string);
                }
            }

            for (int i = 0; i < this.fileList.Items.Count; i++)
            {
                sender.AddLocalFile(this.fileList.Items[i] as string);
            }
            string result = "";
            ShowResultForm resultForm = new ShowResultForm();
            if (sender.Connect())
            {
                if (sender.Send())
                {
                    result = sender.ResponseText;
                }
                else
                {
                    result = "upload final.";
                }
                sender.Close();
            }
            else
            {
                result = "can't connect the url.";
            }
            resultForm.Show();
            resultForm.setResponse(result);
        }

        private void btnClose_Click(object sender, EventArgs e)
        {
            Dispose();
            Close();
        }
    }
}