package com.nxtv.pult;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        // Связываем ViewPager с фрагментами
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return new ConnectFragment(); // Первая вкладка: Подключения
                } else {
                    return new PultFragment();    // Вторая вкладка: Пульт
                }
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        // Настраиваем названия вкладок
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("ПОДКЛЮЧЕНИЯ");
            } else {
                tab.setText("ПУЛЬТ");
            }
        }).attach();
    }
}
