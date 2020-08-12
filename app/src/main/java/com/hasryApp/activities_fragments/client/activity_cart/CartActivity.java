package com.hasryApp.activities_fragments.client.activity_cart;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.hasryApp.R;
import com.hasryApp.activities_fragments.client.activity_checkout.CheckoutActivity;
import com.hasryApp.activities_fragments.client.activity_client_order_details.ClientOrderDetailsActivity;
import com.hasryApp.adapters.CartAdapter;
import com.hasryApp.databinding.ActivityCartBinding;
import com.hasryApp.interfaces.Listeners;
import com.hasryApp.language.Language;
import com.hasryApp.models.CreateOrderModel;
import com.hasryApp.models.ItemCartModel;
import com.hasryApp.models.OrderModel;
import com.hasryApp.models.UserModel;
import com.hasryApp.preferences.Preferences;
import com.hasryApp.share.Common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.paperdb.Paper;

public class CartActivity extends AppCompatActivity implements Listeners.BackListener{
    private ActivityCartBinding binding;
    private String lang;
    private CreateOrderModel createOrderModel;
    private Preferences preferences;
    private UserModel userModel;
    private CartAdapter adapter;
    private List<ItemCartModel> itemCartModelList;
    private double total=0;
    private boolean isDataChanged = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        Paper.init(newBase);
        super.attachBaseContext(Language.updateResources(newBase, Paper.book().read("lang","ar")));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_cart);
        initView();
    }



    private void initView()
    {
        itemCartModelList = new ArrayList<>();
        preferences = Preferences.getInstance();
        userModel = preferences.getUserData(this);
        createOrderModel = preferences.getCartData(this);


        Paper.init(this);
        lang = Paper.book().read("lang", Locale.getDefault().getLanguage());
        binding.setBackListener(this);
        binding.setLang(lang);

        adapter = new CartAdapter(itemCartModelList,this);
        binding.recView.setLayoutManager(new LinearLayoutManager(this));
        binding.recView.setAdapter(adapter);
        updateUi();


        binding.btnCheckout.setOnClickListener(v -> {
            if (userModel!=null){
                Intent intent = new Intent(this, CheckoutActivity.class);
                startActivityForResult(intent,100);
            }else {
                Common.CreateDialogAlert(this,getString(R.string.please_sign_in_or_sign_up));
            }

        });


    }

    private void updateUi() {
        if (createOrderModel!=null){
            itemCartModelList.addAll(createOrderModel.getProducts());
            adapter.notifyDataSetChanged();
            binding.llEmptyCart.setVisibility(View.GONE);
            binding.consTotal.setVisibility(View.VISIBLE);
            calculateTotal();
        }else {
            binding.llEmptyCart.setVisibility(View.VISIBLE);
            binding.consTotal.setVisibility(View.GONE);

        }
    }

    private void calculateTotal() {
        total = 0;
        for (ItemCartModel model:itemCartModelList){

            total += model.getAmount()*model.getPrice();

        }

        binding.tvTotal.setText(String.format(Locale.ENGLISH,"%s %s",String.valueOf(total),getString(R.string.sar)));
    }


    @Override
    public void back() {
        if (isDataChanged){
            Log.e("gg","yyy");
            setResult(RESULT_OK);
        }
        finish();
    }

    public void increase_decrease(ItemCartModel model, int adapterPosition) {
        itemCartModelList.set(adapterPosition,model);
        adapter.notifyItemChanged(adapterPosition);
        createOrderModel.addNewProduct(model);
        preferences.create_update_cart(this,createOrderModel);
        calculateTotal();
    }

    public void deleteItem(ItemCartModel model2, int adapterPosition) {
        itemCartModelList.remove(adapterPosition);
        adapter.notifyItemRemoved(adapterPosition);
        createOrderModel.removeProduct(adapterPosition);
        preferences.create_update_cart(this,createOrderModel);
        isDataChanged = true;
        calculateTotal();
        if (itemCartModelList.size()==0){
            binding.llEmptyCart.setVisibility(View.VISIBLE);
            binding.consTotal.setVisibility(View.GONE);
            preferences.clearCart(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==100&&resultCode==RESULT_OK&&data!=null){
            OrderModel orderModel = (OrderModel) data.getSerializableExtra("data");
            navigateToOrderDetailsActivity(orderModel);
            preferences.clearCart(this);
            updateUi();
            finish();
        }
    }

    private void navigateToOrderDetailsActivity(OrderModel orderModel) {
        Intent intent = new Intent(this, ClientOrderDetailsActivity.class);
        intent.putExtra("data",orderModel);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        back();
    }
}
