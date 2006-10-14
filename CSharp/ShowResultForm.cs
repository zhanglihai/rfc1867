using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;

namespace multi_http_send
{
    public partial class ShowResultForm : Form
    {
        public ShowResultForm()
        {
            InitializeComponent();
        }

        public void setResponse(string text)
        {
            this.richTextBox.Text = text;
        }

        private void btnClose_Click(object sender, EventArgs e)
        {
            Dispose();
            this.Close();

        }
    }
}