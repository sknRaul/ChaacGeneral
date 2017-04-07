package cachalote.blogspot.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class searchDevice extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    //Adaptador del Bluetooth del dispositivo a la aplicacion
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ListView closeDevicesSearch;
    private ArrayAdapter adapter;
    private Button searchDevices;

    /**
     * Metodo para saber cuando el usuario toca un boton
     */
    public View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == searchDevices){
                adapter.clear();//Limpia el adaptador del ListView para evitar que se duplique información
                scanPairedDevices();
            }
        }
    };
    /**
     * Metodo para saber cuando el usario selecciona un elemento del ListView
     */
    public AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String info = ((TextView) view).getText().toString();//Obtiene el texto del ListView
            String MACAddress = info.substring(info.length() - 17);//Extrae solo la direccion MAC
            goToMainActivity(MACAddress);//Envia la direccion MAC al activity principal
            messageToast(getString(R.string.device_selected));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_device);

        /**
         * Instancia de elementos
         */
        closeDevicesSearch = (ListView)findViewById(R.id.close_device_search);
        searchDevices = (Button)findViewById(R.id.button_discover_action);
        adapter = new ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1);
        closeDevicesSearch.setAdapter(adapter);
        /**
         * Metodos a ejecutar en la creación
         */
        scanPairedDevices();
        getBluetoothStatus();

        /**
         * Envio de listeners
         */
        closeDevicesSearch.setOnItemClickListener(onItemClickListener);
        searchDevices.setOnClickListener(onClickListener);
    }

    /**
     * Obtine la información del bluetooth
     */
    public void getBluetoothStatus(){
        if(mBluetoothAdapter == null) {//Revisa si el bluetooth es compatible
            // El dispositivo no esta disponible
        }else if(!mBluetoothAdapter.isEnabled()){//Si el bluetooth no esta activado pide que lo active
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * Busca los dispositivos ya emparejados y los muestra en un listView
     */
    public void scanPairedDevices(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){//Si existe alguan dispositivo sicronizado lo muestra en un listView
            for (BluetoothDevice device : pairedDevices){//Agrega el nombre de los dispositivos ya sincronizados
                adapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }


    public void messageToast(String message){
        Toast.makeText(searchDevice.this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Muestra el layout principal y le envia la direccion MAC seleccionada por el usuaio
     * @param address
     */
    public void goToMainActivity(String address){
        Intent intent = new Intent(this, MainActivity.class);
        finish();
        intent.putExtra("address",address);
        startActivity(intent);
    }
}

